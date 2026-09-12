package org.maven.service;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝服务接口：下单付款、退款、状态查询
 */
public interface AlipayService {

    /**
     * 创建支付宝订单并生成支付页面（电脑网站支付 alipay.trade.page.pay）
     *
     * @param outTradeNo 商户订单号（下单与查询）
     * @param pname      商品名称
     * @param amount     支付金额（元）
     * @return 支付宝支付表单 HTML（前端直接写入页面 → 支付宝收银台）
     */
    String createPayOrder(String outTradeNo, String pname, Double amount);

    /**
     * 退款（alipay.trade.refund，按支付宝交易号 trade_no 退款，支持多次部分退款）
     *
     * @param tradeNo      支付宝交易号 trade_no（订单 transactionId）
     * @param amount       本次退款金额（元）
     * @param outRequestNo 退款请求号（部分退款必传，标识一次退款，唯一）
     * @param reason       退款原因（可选）
     * @return 退款结果信息（success、subMsg...）
     */
    Map<String, Object> refund(String tradeNo, BigDecimal amount, String outRequestNo, String reason);

    /**
     * 交易状态查询（alipay.trade.query）
     */
    Map<String, Object> queryTradeStatus(String orderNo);

    /**
     * 处理支付宝异步通知
     */
    void handleNotify(HttpServletRequest request);
}
