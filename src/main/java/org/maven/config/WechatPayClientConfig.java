package org.maven.config;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.util.PemUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 微信支付客户端
 */

@Configuration
public class WechatPayClientConfig {

    private final WechatPayProperties wechatPayProperties;
    private final ResourceLoader resourceLoader;

    public WechatPayClientConfig(WechatPayProperties wechatPayProperties,
                                 ResourceLoader resourceLoader) {
        this.wechatPayProperties = wechatPayProperties;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public RSAAutoCertificateConfig wechatPayConfig() {
        String privateKeyContent = loadPrivateKeyContent(wechatPayProperties.getPrivateKeyPath());


        return new RSAAutoCertificateConfig.Builder()
                .merchantId(wechatPayProperties.getMchId())
                .privateKey(PemUtil.loadPrivateKeyFromString(privateKeyContent))
                .merchantSerialNumber(wechatPayProperties.getMerchantSerialNumber())
                .apiV3Key(wechatPayProperties.getApiV3Key())
                .build();
    }

    /**
     * 加载私钥
     */
    private String loadPrivateKeyContent(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("加载微信支付私钥文件失败: " +
                    path, e);
        }
    }
}
