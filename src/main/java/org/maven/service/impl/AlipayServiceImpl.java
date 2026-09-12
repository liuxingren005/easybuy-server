package org.maven.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maven.config.AlipayProperties;
import org.maven.service.AlipayService;
import org.maven.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付服务实现类
 * 电脑网站支付（alipay.trade.page.pay）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;

    private final OrderService orderService;

    @Override
    public String createPayOrder(String outTradeNo, String pname, Double amount) {
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            // 异步回调地址
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            // 同步跳转地址
            request.setReturnUrl(alipayProperties.getReturnUrl());

            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            // 商户订单号（由调用方统一生成，下单与查询共用）
            model.setOutTradeNo(outTradeNo);
            // 订单标题
            model.setSubject(pname);
            // 订单总金额（元）
            model.setTotalAmount(String.format("%.2f", amount));
            // 销售产品码
            model.setProductCode("FAST_INSTANT_TRADE_PAY");

            request.setBizModel(model);

            // 生成支付表单HTML
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝创建支付订单失败", e);
            throw new RuntimeException("支付宝支付创建失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refund(String tradeNo, BigDecimal amount, String outRequestNo, String reason) {
        Map<String, Object> result = new HashMap<>();

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        // 按支付宝交易号 trade_no 退款（transactionId = trade_no）
        model.setTradeNo(tradeNo);
        model.setRefundAmount(amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        // 退款请求号：部分退款必传，标识一次退款请求，唯一；相同请求号重复调用幂等
        if (outRequestNo != null && !outRequestNo.isEmpty()) {
            model.setOutRequestNo(outRequestNo);
        }
        model.setRefundReason(reason != null && !reason.isEmpty() ? reason : "正常退款");
        request.setBizModel(model);

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                result.put("success", true);
                result.put("tradeNo", response.getTradeNo());
                result.put("refundFee", response.getRefundFee());
                result.put("gmtRefundPay", response.getGmtRefundPay());
                log.info("支付宝退款成功，交易号：{}，退款金额：{}，退款请求号：{}", tradeNo, amount, outRequestNo);
                return result;
            }
            // 业务异常
            result.put("success", false);
            result.put("msg", response.getMsg());
            result.put("subMsg", response.getSubMsg());
            log.error("支付宝退款失败，交易号：{}，错误：{}", tradeNo, response.getSubMsg());
            return result;
        } catch (AlipayApiException e) {
            // 网络/网关异常
            log.warn("支付宝退款请求网络异常，交易号：{}，退款请求号：{}，原因：{}",
                    tradeNo, outRequestNo, e.getMessage());
        }

        // 调用退款查询接口
        log.warn("支付宝退款无响应，改为查询退款状态，交易号：{}，退款请求号：{}", tradeNo, outRequestNo);
        Map<String, Object> queryResult = queryRefundResult(tradeNo, outRequestNo);
        if (Boolean.TRUE.equals(queryResult.get("success"))) {
            // 支付宝退款成功：继续落库
            result.put("success", true);
            result.put("tradeNo", queryResult.getOrDefault("tradeNo", tradeNo));
            result.put("refundFee", queryResult.get("refundAmount"));
            log.info("支付宝退款经查询确认成功，交易号：{}，退款金额：{}，退款请求号：{}",
                    tradeNo, queryResult.get("refundAmount"), outRequestNo);
            return result;
        }

        // 支付宝退款失败：重新发起
        log.error("支付宝退款确认失败，交易号：{}，退款请求号：{}", tradeNo, outRequestNo);
        result.put("success", false);
        result.put("msg", "支付宝退款请求超时或网络异常，且未查询到退款记录，请稍后重试或联系客服核对");
        return result;
    }

    /**
     * 退款结果查询（alipay.trade.fastpay.refund.query）
     */
    private Map<String, Object> queryRefundResult(String tradeNo, String outRequestNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
            model.setTradeNo(tradeNo);
            model.setOutRequestNo(outRequestNo);
            request.setBizModel(model);

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess() && "REFUND_SUCCESS".equals(response.getRefundStatus())) {
                result.put("success", true);
                result.put("tradeNo", response.getTradeNo());
                result.put("refundAmount", response.getRefundAmount());
            } else {
                // 超时
                result.put("success", false);
                result.put("subCode", response.getSubCode());
                result.put("subMsg", response.getSubMsg());
                log.warn("支付宝退款查询未找到退款记录，交易号：{}，退款请求号：{}，subCode：{}",
                        tradeNo, outRequestNo, response.getSubCode());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝退款查询异常，交易号：{}，退款请求号：{}", tradeNo, outRequestNo, e);
            result.put("success", false);
        }
        return result;
    }

    /**
     * 主动查询
     */
    @Override
    public Map<String, Object> queryTradeStatus(String orderNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                result.put("success", true);
                result.put("tradeStatus", response.getTradeStatus());
                result.put("tradeNo", response.getTradeNo());
                result.put("totalAmount", response.getTotalAmount());
                result.put("sendPayDate", response.getSendPayDate());
                // 交易状态：WAIT_BUYER_PAY（待支付）、TRADE_SUCCESS（支付成功）、TRADE_FINISHED（交易完成）、TRADE_CLOSED（交易关闭）
                log.info("支付宝交易查询成功，订单号：{}，状态：{}", orderNo, response.getTradeStatus());

                // 查询即同步：根据支付宝状态更新本地订单
                String tradeStatus = response.getTradeStatus();
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 同步：幂等场景（异步通知/同步跳转已更新）
                    try {
                        orderService.payOrderByOutTradeNo(orderNo, 1, response.getTradeNo());
                        log.info("支付宝状态同步成功，订单号：{}，交易号：{}", orderNo, response.getTradeNo());
                    } catch (Exception e) {
                        log.warn("支付宝状态同步本地订单失败，订单号：{}", orderNo, e);
                    }
                }
            } else {
                result.put("success", false);
                result.put("msg", response.getMsg());
                result.put("subMsg", response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝交易查询异常", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleNotify(HttpServletRequest request) {
        try {
            // 获取请求数据（支付宝POST回调）
            Map<String, String> params = getStringMap(request);

            // 验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );

            if (!signVerified) {
                log.error("支付宝支付通知验签失败");
                throw new RuntimeException("支付宝支付通知验签失败");
            }

            // 商户订单号
            String outTradeNo = params.get("out_trade_no");
            // 支付宝交易号
            String tradeNo = params.get("trade_no");
            // 交易状态
            String tradeStatus = params.get("trade_status");

            log.info("支付宝支付通知验证成功，商户订单号：{}，交易状态：{}",
                    outTradeNo, tradeStatus);

            // 成功状态判断
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                orderService.payOrderByOutTradeNo(outTradeNo, 1, tradeNo); // 落库
                log.info("支付宝支付成功，更新订单状态：outTradeNo={}, tradeNo={}",
                        outTradeNo, tradeNo);
            }
        } catch (AlipayApiException e) {
            log.error("支付宝支付通知处理异常", e);
            throw new RuntimeException("支付宝支付通知处理异常：" + e.getMessage(), e);
        }
    }

    private static Map<String, String> getStringMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }
        return params;
    }
}
