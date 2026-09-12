package org.maven.service;

import org.maven.exception.BusinessException;

/**
 * 邮箱验证码服务接口
 *
 * 存储：Redis，有效期 + 发送频率
 */
public interface EmailService {

    /**
     * 发送邮箱验证码
     *
     * @param email 收件邮箱
     * @param scene 业务场景（register / changePassword / login）
     * @return 验证码
     */
    String sendCode(String email, String scene);

    /**
     * 校验邮箱验证码（一次性使用）
     *
     * @param email 邮箱
     * @param scene 业务场景
     * @param code  用户输入的验证码
     * @throws BusinessException 验证码错误 / 已过期 / 参数缺失
     */
    void verifyCode(String email, String scene, String code);
}
