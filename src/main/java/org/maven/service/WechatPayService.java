package org.maven.service;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付服务接口：下单付款（Native扫码）
 */
public interface WechatPayService {

    /**
     * 创建微信支付订单（Native支付，返回二维码链接 code_url）
     *
     * @param pid    商品ID
     * @param pname  商品名称
     * @param amount 支付金额（元）
     * @return order_no、code_url
     */
    Map<String, Object> createPayOrder(Integer pid, String pname, Double amount);

    /**
     * 交易状态查询
     */
    Map<String, Object> queryTradeStatus(String orderNo);

    /**
     * 退款（按微信交易号 transaction_id，支持多次部分退款）
     *
     * @param transactionId 微信交易号（对应订单 transactionId）
     * @param amount        本次退款金额（元）
     * @param totalAmount   订单原支付总金额（元，微信退款单 - total）
     * @param outRefundNo   商户退款单号（唯一，标识一次退款）
     * @param reason        退款原因（可选）
     * @return 退款结果信息（success、msg...）
     */
    Map<String, Object> refund(String transactionId, BigDecimal amount, BigDecimal totalAmount,
                               String outRefundNo, String reason);

    /**
     * 处理微信支付异步通知
     * （验签 + 解密 + 解析）
     */
    void handleNotify(HttpServletRequest request);

}
