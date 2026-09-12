package org.maven.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maven.config.EmailCodeProperties;
import org.maven.exception.BusinessException;
import org.maven.service.EmailService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    /**
     * Redis 验证码 key 前缀 email:code:{scene}:{email}
     */
    private static final String CODE_KEY_PREFIX = "email:code:";

    /**
     * Redis 发送频率锁 key 前缀 email:lock:{scene}:{email}
     */
    private static final String LOCK_KEY_PREFIX = "email:lock:";

    private final JavaMailSender mailSender;

    private final EmailCodeProperties properties;

    private final RedisTemplate<String, Object> redisTemplate;

    // 加密级
    private final SecureRandom random = new SecureRandom();

    @Override
    public String sendCode(String email, String scene) {
        // 1. 频率控制：interval 秒内不允许重复发送（NX + SETEX），“防抖”
        String lockKey = LOCK_KEY_PREFIX + scene + ":" + email;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", properties.getIntervalSeconds(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("发送过于频繁，请稍后再试");
        }

        // 2. 生成 6 位数字验证码
        String code = String.format("%06d", random.nextInt(1_000_000)); // 视觉

        // 3. 写入 Redis（设置过期时间）
        String codeKey = CODE_KEY_PREFIX + scene + ":" + email;
        redisTemplate.opsForValue().set(codeKey, code, properties.getExpireSeconds(), TimeUnit.SECONDS);

        // 4. 尝试发送邮件（失败不阻断，降级联调：邮件发不出去，注册接口依然返回成功）
        try {
            sendPlainTextEmail(email, code);
        } catch (Exception e) {
            log.warn("邮件发送失败（邮箱={}，场景={}）：{}",
                    email, scene, e.getMessage());
        }

        // 5. 开发联调用
        log.info("【邮箱验证码】邮箱:{}，场景:{}，验证码:{}，有效期:{}分钟",
                email,
                scene,
                code,
                properties.getExpireSeconds() / 60);

        return code;
    }

    @Override
    public void verifyCode(String email, String scene, String code) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("邮箱不能为空");
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException("邮箱验证码不能为空");
        }
        String codeKey = CODE_KEY_PREFIX + scene + ":" + email;
        Object saved = redisTemplate.opsForValue().get(codeKey);
        if (saved == null) {
            throw new BusinessException("邮箱验证码已过期，请重新获取");
        }
        if (!code.trim().equals(saved.toString())) {
            throw new BusinessException("邮箱验证码错误");
        }
        // 一次性使用
        redisTemplate.delete(codeKey);
    }

    /**
     * 发送 plain-text 邮件
     * （精美模板 Thymeleaf 渲染 .html 模板文件）
     */
    private void sendPlainTextEmail(String email, String code) throws Exception {
        int expireMinutes = properties.getExpireSeconds() / 60;
        String text = "【易买网】验证码：" + code
                + "，有效期" + expireMinutes + "分钟。"
                + "请勿泄露，非本人操作请忽略。";

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setFrom(properties.getFrom());
        helper.setTo(email);
        helper.setSubject(properties.getSubject());
        helper.setText(text, false);

        mailSender.send(mimeMessage);
    }
}
