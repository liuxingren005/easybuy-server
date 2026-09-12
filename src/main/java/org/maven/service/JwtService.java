package org.maven.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.maven.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT JSON Web Token 令牌服务类（签发/解析/延期）
 * Header.Payload.Signature
 * Authorization: Bearer <token>
 * 会话跟踪 userId、loginName、type
 */
@Component
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成令牌
     */
    public String generate(Integer userId, String loginName, Integer type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("loginName", loginName);
        claims.put("type", type);

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims) // 自定义载荷
                .setSubject(String.valueOf(userId)) // 主题（通常为用户ID）
                .setIssuedAt(new Date(now)) // 签发时间
                .setExpiration(new Date(now + jwtProperties.getExpire() * 1000L)) // 过期时间
                .signWith(secretKey) // 密钥签名
                .compact(); // JWT
    }

    /**
     * 解析令牌
     * @param token jwt
     * @return Claims 载荷：JWT “数据载体”（Header.Payload.Signature 第二部分）
     * @throws ExpiredJwtException token已过期
     * @throws JwtException 签名错误、格式非法、密钥不匹配
     */
    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 综合：签名、过期
     */
    public boolean isLegal(String token) {
        try {
            Claims claims = parse(token);
            return claims != null;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 会话延期：旧令牌载荷重新签发
     * JWT 一经签发不可改期，延期 = 换发新令牌
     * @param token 旧 jwt
     * @return 新 jwt
     */
    public String refresh(String token) {
        try {
            Claims claims = parse(token);
            if (claims == null) {
                return null;
            }
            return generate(claims.get("userId", Integer.class),
                    claims.get("loginName", String.class),
                    claims.get("type", Integer.class));
        } catch (JwtException e) {
            return null;
        }
    }
}
