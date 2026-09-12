package org.maven.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {

    /** 微信AppID */

    private String appid;

    /** 商户号 */

    private String mchId;

    /** APIv3密钥（32位）*/

    private String apiV3Key;

    /** 商户API私钥文件路径 */

    private String privateKeyPath;

    /** 商户API证书序列号 */

    private String merchantSerialNumber;

    /** 异步回调地址 */

    private String notifyUrl;

}
