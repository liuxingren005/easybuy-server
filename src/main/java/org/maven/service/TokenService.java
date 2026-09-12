package org.maven.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.maven.config.JwtProperties;
import org.maven.entity.User;
import org.maven.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 令牌会话服务（JWT + Redis 会话跟踪）
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    /**
     * Redis 会话 key 前缀 - （令牌 → 用户信息）
     */
    private static final String TOKEN_KEY_PREFIX = "login:token:";

    /**
     * 用户 → 当前令牌映射 key 前缀（同账号互踢）- （用户 → 当前令牌）
     */
    private static final String USER_KEY_PREFIX = "login:user:";

    /**
     * 被踢下线标记 key 前缀
     */
    private static final String KICKED_KEY_PREFIX = "login:kicked:";

    /**
     * 会话延期互斥锁 key 前缀
     */
    private static final String REFRESH_LOCK_PREFIX = "lock:refresh:";

    /**
     * 登录互斥锁 key 前缀（同账号并发）
     */
    private static final String LOGIN_LOCK_PREFIX = "lock:login:";

    private final JwtService jwtService;

    private final JwtProperties jwtProperties;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建令牌并写入 Redis 会话（同账号互踢）
     * 用户级分布式锁
     */
    public String create(User user) {
        String token = jwtService.generate(
                user.getId(), user.getLoginName(), user.getType()
        );
        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(), user.getLoginName(), user.getType()
        );
        // 同账号互踢：删除该用户旧会话，并标记被踢
        String userKey = USER_KEY_PREFIX + user.getId();
        Object oldToken = redisTemplate.opsForValue().get(userKey);
        if (oldToken instanceof String old && !old.isBlank()) { // 模式匹配
            String oldKey = TOKEN_KEY_PREFIX + old;
            if (redisTemplate.hasKey(oldKey)) {
                redisTemplate.delete(oldKey);
                redisTemplate.opsForValue().set(KICKED_KEY_PREFIX + old, "1", 300, TimeUnit.SECONDS); // 状态标志位
            }
        }
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, userPrincipal,
                jwtProperties.getExpire(), TimeUnit.SECONDS);
        // 用户当前令牌
        redisTemplate.opsForValue().set(userKey, token, jwtProperties.getExpire(), TimeUnit.SECONDS);
        return token;
        /*String lockKey = LOGIN_LOCK_PREFIX + user.getId();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new IllegalStateException("登录中，请稍候重试");
        }
        try {
            String token = jwtService.generate(
                    user.getId(), user.getLoginName(), user.getType()
            );
            UserPrincipal userPrincipal = new UserPrincipal(
                    user.getId(), user.getLoginName(), user.getType()
            );
            // 同账号互踢：删除该用户旧会话，并标记被踢
            String userKey = USER_KEY_PREFIX + user.getId();
            Object oldToken = redisTemplate.opsForValue().get(userKey);
            if (oldToken instanceof String old && !old.isBlank()) { // 模式匹配
                String oldKey = TOKEN_KEY_PREFIX + old;
                if (redisTemplate.hasKey(oldKey)) {
                    redisTemplate.delete(oldKey);
                    redisTemplate.opsForValue().set(KICKED_KEY_PREFIX + old, "1", 300, TimeUnit.SECONDS);
                }
            }
            redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, userPrincipal,
                    jwtProperties.getExpire(), TimeUnit.SECONDS);
            // 用户当前令牌
            redisTemplate.opsForValue().set(userKey, token, jwtProperties.getExpire(), TimeUnit.SECONDS);
            return token;
        } finally {
            redisTemplate.delete(lockKey);
        }*/
    }

    /**
     * 校验令牌并返回登录用户；有效时顺带续期
     */
    public UserPrincipal verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        // JWT 校验（签名错误/过期统一视为无效 → 401）
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (JwtException e) {
            log.warn("JWT 校验失败：{}", e.getMessage());
            return null;
        }
        if (claims == null) {
            return null;
        }
        // Redis 会话校验
        String key = TOKEN_KEY_PREFIX + token;
        Object sessionData = redisTemplate.opsForValue().get(key);
        if (!(sessionData instanceof UserPrincipal userPrincipal)) {
            return null;
        }
        // 续期（会话与用户令牌映射同步续期，保持互踢链路）
        redisTemplate.expire(key, jwtProperties.getExpire(), TimeUnit.SECONDS);
        redisTemplate.expire(USER_KEY_PREFIX + userPrincipal.getUserId(), jwtProperties.getExpire(), TimeUnit.SECONDS);
        return userPrincipal;
    }

    /**
     * 删除令牌会话（退出登录）
     */
    public void remove(String token) {
        if (token != null && !token.isBlank()) {
            String key = TOKEN_KEY_PREFIX + token;
            // 仅当用户令牌映射指向当前令牌时清除，避免误删新登录的映射
            Object sessionData = redisTemplate.opsForValue().get(key);
            if (sessionData instanceof UserPrincipal principal) {
                String userKey = USER_KEY_PREFIX + principal.getUserId();
                if (token.equals(redisTemplate.opsForValue().get(userKey))) {
                    redisTemplate.delete(userKey);
                }
            }
            redisTemplate.delete(key);
        }
    }

    /**
     * 令牌是否被踢下线（账号在其他设备登录）
     */
    public boolean isKicked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return redisTemplate.hasKey(KICKED_KEY_PREFIX + token);
    }

    /**
     * 从请求头中解析令牌（去掉 Bearer 前缀）
     */
    public String resolve(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String prefix = jwtProperties.getPrefix();
        if (prefix != null && !prefix.isBlank() && headerValue.startsWith(prefix)) {
            return headerValue.substring(prefix.length());
        }
        return headerValue;
    }

    /**
     * 会话延期：剩余有效期不足一半时换发新令牌并迁移 Redis 会话
     */
    public String refreshSession(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String key = TOKEN_KEY_PREFIX + token;
        // 剩余 TTL Time To Live（存活时间）
        long remain = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        // 会话不存在或剩余充足，不延期
        if (remain < 0 || remain > jwtProperties.getExpire() / 2) {
            return null;
        }
        // 并发互斥：同一旧令牌只允许一个请求执行换发，其余请求继续用旧令牌 - 原子操作：SET refresh_lock:{token} "1" NX EX 30
        /**
         * SET refresh_lock:{token} "1" NX EX 30
         *  │   │                      │  │  │  │
         *  │   │                      │  │  │  └── 过期时间：30
         *  │   │                      │  │  └───── 时间单位：(EXpire)
         *  │   │                      │  └──────── 写入条件：NX = 仅当Key不存在时写入 (Not eXists)
         *  │   │                      └─────────── Value："1"（锁占位符）
         *  │   └────────────────────────────────── Key：refresh_lock:{token}
         *  └────────────────────────────────────── Command：SET
         */
        Boolean isLock = redisTemplate.opsForValue()
                .setIfAbsent(REFRESH_LOCK_PREFIX + token, "1", 30, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(isLock)) {
            return null;
        }
        // 换发新令牌并迁移会话
        String newToken = jwtService.refresh(token);
        if (newToken == null) {
            return null;
        }
        Object sessionData = redisTemplate.opsForValue().get(key);
        if (sessionData == null) {
            return null;
        }
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + newToken, sessionData,
                jwtProperties.getExpire(), TimeUnit.SECONDS);
        // 同步用户令牌映射至新令牌，后续登录能踢掉当前会话
        if (sessionData instanceof UserPrincipal principal) {
            redisTemplate.opsForValue().set(USER_KEY_PREFIX + principal.getUserId(), newToken,
                    jwtProperties.getExpire(), TimeUnit.SECONDS);
        }
        // 旧令牌宽限期，旧令牌请求平稳落地
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        return newToken;
    }
}
