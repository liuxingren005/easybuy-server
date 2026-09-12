package org.maven.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maven.common.ResponseResult;
import org.maven.entity.Order;
import org.maven.service.OrderService;
import org.maven.service.WechatPayService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/wechat")
@RequiredArgsConstructor
public class WechatPayController {

    private final WechatPayService wechatPayService;
    private final OrderService orderService;

    /**
     * 创建微信支付订单（Native扫码支付）
     * GET /wechat/pay?orderId=xxx
     * 返回二维码链接 code_url
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

        Map<String, Object> result = wechatPayService.createPayOrder(
                order.getId(),
                "易买网订单-" + order.getSerialNumber(),
                order.getCost().doubleValue()
        );

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseResult.success().put("data", result);
        } else {
            return ResponseResult.error((String) result.get("msg"));
        }
    }

    /**
     * 微信支付异步通知
     * POST /wechat/notify
     * JSON {"code":"SUCCESS"} / {"code":"FAIL"}（微信格式）
     */
    @PostMapping("/notify")
    public Map<String, String> notify(HttpServletRequest request) {
        log.info("收到微信支付异步通知");
        Map<String, String> result = new HashMap<>();
        try {
            wechatPayService.handleNotify(request);
            result.put("code", "SUCCESS"); // ← 停止信号
            result.put("message", "成功");
        } catch (Exception e) {
            log.error("微信支付通知处理失败", e);
            result.put("code", "FAIL");    // ← 重试信号
            result.put("message", "失败");
        }
        return result;
    }

    /**
     * 查询微信支付交易状态 - 主动查询
     * GET /wechat/query?orderNo=xxx
     */
    @GetMapping("/query")
    public ResponseResult query(@RequestParam String orderNo) {
        Map<String, Object> result = wechatPayService.queryTradeStatus(orderNo);
        return ResponseResult.success().put("data", result);
    }
}
