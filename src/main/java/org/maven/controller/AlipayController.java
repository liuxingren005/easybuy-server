package org.maven.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maven.common.ResponseResult;
import org.maven.entity.Order;
import org.maven.service.AlipayService;
import org.maven.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/alipay")
@RequiredArgsConstructor
public class AlipayController {

    private final AlipayService alipayService;

    private final OrderService orderService;

    /**
     * 创建支付宝支付订单（电脑网站支付）
     * GET /alipay/pay?orderId=xxx
     * 返回支付宝支付表单HTML
     */
    @GetMapping("/pay")
    public ResponseResult pay(@RequestParam Integer orderId) {
        Order order = orderService.findById(orderId);
        if (order == null) {
            return ResponseResult.error("订单不存在");
        }
        if (order.getStatus() != 1) {
            return ResponseResult.error("订单状态不正确");
        }

        // 商户订单号，下单与查询共用
        String outTradeNo = "EB" + order.getId() + "-" + System.currentTimeMillis();
        String html = alipayService.createPayOrder(outTradeNo,
                "易买网订单-" + order.getSerialNumber(),
                order.getCost().doubleValue());

        // 返回支付表单HTML和商户订单号（前端用于主动查询）
        Map<String, Object> data = new HashMap<>();
        data.put("html", html);
        data.put("outTradeNo", outTradeNo);
        return ResponseResult.success().put("data", data);
    }

    /**
     * 支付宝支付异步通知
     * POST /alipay/notify
     * 文本 "success" / "failure"（支付宝格式）
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        log.info("收到支付宝异步通知");
        try {
            alipayService.handleNotify(request);
            return "success"; // // ← 停止信号：支付宝收到停止重试
        } catch (Exception e) {
            log.error("支付宝支付通知处理失败", e);
            return "failure"; // // ← 重试信号：支付宝会按节奏重发
        }
    }

    /**
     * 支付宝支付同步跳转（支付完成后跳转回网站）- 浏览器发起 GET 请求
     * GET /alipay/return
     */
    @GetMapping("/return")
    public void returnUrl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("支付宝同步跳转");
        response.setContentType("text/html;charset=UTF-8");
        // 使用前端绝对路径避免后端跳转异常，并透传支付宝回调订单ID
        String outTradeNo = request.getParameter("out_trade_no"); // 定位订单
        String orderId = "";
        if (outTradeNo != null && outTradeNo.startsWith("EB")) {
            // 商户订单号格式：EB{orderId}-{timestamp}
            StringBuilder sb = new StringBuilder();
            String remain = outTradeNo.substring(2);
            for (int i = 0; i < remain.length(); i++) {
                if (Character.isDigit(remain.charAt(i))) {
                    sb.append(remain.charAt(i));
                } else {
                    break;
                }
            }
            orderId = sb.toString();
        }

        // 同步：主动查询
        boolean paySuccess = false;
        if (outTradeNo != null) {
            try {
                Map<String, Object> queryResult = alipayService.queryTradeStatus(outTradeNo); // 主动查询
                String tradeStatus = (String) queryResult.get("tradeStatus");
                paySuccess = "TRADE_SUCCESS".equals(tradeStatus)
                        || "TRADE_FINISHED".equals(tradeStatus);
            } catch (Exception e) {
                log.error("同步跳转查询支付宝状态失败", e);
            }
        }

        // 传递
        String redirectUrl = "http://localhost:5173/pay/result?success=" + paySuccess;
        if (!orderId.isEmpty()) {
            redirectUrl += "&orderId=" + orderId;
        }
        response.sendRedirect(redirectUrl);
    }

    /**
     * 查询支付宝交易状态
     * GET /alipay/query?orderNo=xxx
     */
    @GetMapping("/query")
    public ResponseResult query(@RequestParam String orderNo) {
        Map<String, Object> result = alipayService.queryTradeStatus(orderNo);
        return ResponseResult.success().put("data", result);
    }

    /**
     * 支付宝退款（按交易号 trade_no）
     * POST /alipay/refund
     */
    @PostMapping("/refund")
    public ResponseResult refund(@RequestParam String tradeNo, @RequestParam BigDecimal amount) {
        // 退款请求号：唯一标识一次退款（部分退款必传）
        String outRequestNo = "ERF" + System.currentTimeMillis();
        Map<String, Object> result = alipayService.refund(tradeNo, amount, outRequestNo, "正常退款");
        return ResponseResult.success().put("data", result);
    }
}
