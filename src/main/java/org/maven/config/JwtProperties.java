package org.maven.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HS256 密钥
     */
    private String secret;

    /**
     * 令牌有效期
     */
    private Long expire;

    /**
     * 请求头名称
     */
    private String header;

    /**
     * 令牌 JWT
     */
    private String prefix;
}
