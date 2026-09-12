package org.maven.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码
 *
 * 来源：application.yml → email.code.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "email.code")
public class EmailCodeProperties {

    /**
     * 验证码有效期（秒）
     */
    private Integer expireSeconds;

    /**
     * 同一邮箱发送频率（秒）
     */
    private Integer intervalSeconds;

    /**
     * 邮件标题
     */
    private String subject;

    /**
     * 发件人
     */
    private String from;
}
