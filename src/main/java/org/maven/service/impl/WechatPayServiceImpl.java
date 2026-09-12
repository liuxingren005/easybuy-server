package org.maven.service.impl;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maven.config.WechatPayProperties;
import org.maven.service.OrderService;
import org.maven.service.WechatPayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付服务实现类
 * Native扫码支付
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPayServiceImpl implements WechatPayService {

    private final RSAAutoCertificateConfig wechatPayConfig;
    private final WechatPayProperties wechatPayProperties;

    private final OrderService orderService;

    @Override
    public Map<String, Object> createPayOrder(Integer pid, String pname, Double amount) {
        Map<String, Object> result = new HashMap<>();
        try {
            NativePayService service = new NativePayService.Builder()
                    .config(wechatPayConfig)
                    .build();

            PrepayRequest request = new PrepayRequest();
            // 微信AppID
            request.setAppid(wechatPayProperties.getAppid());
            // 商户号
            request.setMchid(wechatPayProperties.getMchId());
            // 商户订单号
            String outTradeNo = "EB" + pid + "-" + System.currentTimeMillis();
            request.setOutTradeNo(outTradeNo);
            // 商品描述
            request.setDescription(pname);
            // 回调地址
            request.setNotifyUrl(wechatPayProperties.getNotifyUrl());
            // 金额（单位：分）
            Amount amountObj = new Amount();
            amountObj.setTotal(BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100)).intValue());
            request.setAmount(amountObj);

            PrepayResponse response = service.prepay(request);

            result.put("success", true);
            result.put("outTradeNo", outTradeNo);
            result.put("codeUrl", response.getCodeUrl());
            result.put("codeUrlLink", response.getCodeUrl());
            log.info("微信支付创建订单成功，订单号：{}，金额：{}",
                    outTradeNo, amount);
        } catch (Exception e) {
            log.error("微信支付创建订单失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> queryTradeStatus(String orderNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            NativePayService service = new NativePayService.Builder()
                    .config(wechatPayConfig)
                    .build();

            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setOutTradeNo(orderNo);
            request.setMchid(wechatPayProperties.getMchId());

            Transaction transaction = service.queryOrderByOutTradeNo(request); // 主动查询

            result.put("success", true);
            result.put("tradeState", transaction.getTradeState());
            result.put("tradeStateDesc", transaction.getTradeStateDesc());
            result.put("transactionId", transaction.getTransactionId());
            result.put("amount", transaction.getAmount() != null ? transaction.getAmount().getTotal() : null);
            result.put("successTime", transaction.getSuccessTime());
            // 交易状态：SUCCESS（支付成功）、REFUND（退款）、NOTPAY（未支付）、CLOSED（已关闭）、PAYERROR（支付失败）
            log.info("微信支付查询订单成功，订单号：{}，状态：{}", orderNo, transaction.getTradeState());

            // 同步：根据微信状态更新本地订单（幂等）
            if (transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                try {
                    orderService.payOrderByOutTradeNo(orderNo, 2, transaction.getTransactionId());
                    log.info("微信支付状态同步成功，订单号：{}，交易号：{}", orderNo, transaction.getTransactionId());
                } catch (Exception e) {
                    log.warn("微信支付状态同步失败，订单号：{}", orderNo, e);
                }
            }
        } catch (Exception e) {
            log.error("微信支付查询订单失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleNotify(HttpServletRequest request) {
        try {
            // 获取请求数据（请求体 + 签名相关请求头）
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String body = sb.toString();

            String serialNumber = request.getHeader("Wechatpay-Serial");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String signature = request.getHeader("Wechatpay-Signature");

            // 验证签名并解密回调
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serialNumber).nonce(nonce).signature(signature).timestamp(timestamp).body(body).build();

            NotificationParser parser = new NotificationParser(wechatPayConfig);
            Transaction transaction = parser.parse(requestParam, Transaction.class);

            log.info("微信支付通知验证成功，商户订单号：{}，交易状态：{}",
                    transaction.getOutTradeNo(), transaction.getTradeState());

            // 成功状态判断
            if (transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                orderService.payOrderByOutTradeNo(
                        transaction.getOutTradeNo(),
                        2,
                        transaction.getTransactionId()
                );
                log.info("微信支付成功，更新订单状态：outTradeNo={}, transactionId={}",
                        transaction.getOutTradeNo(), transaction.getTransactionId());
            }
        } catch (Exception e) {
            log.error("微信支付通知处理异常", e);
            throw new RuntimeException("微信支付通知处理异常：" + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> refund(String transactionId, BigDecimal amount, BigDecimal totalAmount,
                                      String outRefundNo, String reason) {
        Map<String, Object> result = new HashMap<>();
        try {
            RefundService service = new RefundService.Builder().config(wechatPayConfig).build();

            CreateRequest request = new CreateRequest();
            // 按微信交易号 transaction_id 退款（订单 transactionId）
            request.setTransactionId(transactionId);
            // 商户退款单号：全局唯一，多次部分退款 - 各个单号
            request.setOutRefundNo(outRefundNo);
            request.setReason(reason != null && !reason.isEmpty() ? reason : "正常退款");

            AmountReq amt = new AmountReq();
            // 金额单位：分；total 为订单原支付总额，refund 为本次退款金额（部分退款时 refund < total）
            long refundCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            long totalCents = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
            amt.setRefund(refundCents);
            amt.setTotal(totalCents);
            amt.setCurrency("CNY");
            request.setAmount(amt);

            Refund refund = service.create(request);
            result.put("success", true);
            result.put("refundId", refund.getRefundId());
            result.put("outRefundNo", outRefundNo);
            log.info("微信退款成功，交易号：{}，退款单号：{}，退款金额：{}", transactionId, outRefundNo, amount);
        } catch (Exception e) {
            log.error("微信退款异常", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }
}
