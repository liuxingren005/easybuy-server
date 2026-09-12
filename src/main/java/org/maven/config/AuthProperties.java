package org.maven.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限校验配置属性（白名单 + 管理员规则）
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * 全方法白名单
     */
    private List<String> whiteListAll;

    /**
     * GET只读白名单
     */
    private List<String> whiteListGet;
}
