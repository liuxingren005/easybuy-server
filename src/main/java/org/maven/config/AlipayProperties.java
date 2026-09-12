package org.maven.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** 应用APPID */
    private String appId;

    /** 应用私钥（PKCS8格式） */
    private String appPrivateKey;

    /** 支付宝公钥 */
    private String alipayPublicKey;

    /** 网关地址：沙箱 / 正式环境 */
    private String gateway;

    /** 签名类型：RSA2 */
    private String signType;

    /** 编码 */
    private String charset;

    /** 数据格式 */
    private String format;

    /** 异步回调地址 */
    private String notifyUrl;

    /** 同步跳转地址 */
    private String returnUrl;

}
