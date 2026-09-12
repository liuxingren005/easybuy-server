package org.maven.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝客户端
 */
@Configuration
public class AlipayClientConfig {

    private final AlipayProperties alipayProperties;

    public AlipayClientConfig(AlipayProperties alipayProperties) {
        this.alipayProperties = alipayProperties;
    }

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                alipayProperties.getGateway(),
                alipayProperties.getAppId(),
                alipayProperties.getAppPrivateKey(),
                alipayProperties.getFormat(),
                alipayProperties.getCharset(),
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getSignType()
        );
    }
}
