package org.maven.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maven.entity.Order;
import org.maven.entity.OrderDetail;
import org.maven.request.RefundDetail;
import org.maven.exception.BusinessException;
import org.maven.service.AlipayService;
import org.maven.service.OrderService;
import org.maven.service.AfterSaleService;
import org.maven.service.WechatPayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单退款服务实现类
 * 协调第三方退款 API 调用与订单状态更新
 * 支持整单全额退款与按明细多次部分退款
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    private final OrderService orderService;

    private final AlipayService alipayService;
    private final WechatPayService wechatPayService;

    @Override
    public void refundOrder(Integer id) {
        refundOrder(id, null);
    }

    @Override
    public void refundOrder(Integer id, String reason) {
        // 默认：整单全额退款
        refundOrder(id, null, reason);
    }

    @Override
    public void refundOrder(Integer id, List<RefundDetail> refundDetailList, String reason) {
        // 1. 校验订单（存在性、归属、状态、交易号、支付方式）
        Order order = orderService.getOrderForRefund(id);
        List<OrderDetail> details = order.getOrderDetailList();
        if (details == null || details.isEmpty()) {
            throw new BusinessException("缺少订单明细，无法退款");
        }
        Map<Integer, OrderDetail> detailMap = new HashMap<>();
        for (OrderDetail d : details) {
            detailMap.put(d.getId(), d);
        }

        // 2. 解析退款明细
        List<RefundDetail> refundDetails = new ArrayList<>();
        if (refundDetailList == null || refundDetailList.isEmpty()) {
            // 全额退款
            for (OrderDetail d : details) {
                int remaining = d.getQuantity() - refundQuantity(d);
                if (remaining > 0) {
                    RefundDetail item = new RefundDetail();
                    item.setOrderDetailId(d.getId());
                    item.setQuantity(remaining);
                    refundDetails.add(item);
                }
            }
            if (refundDetails.isEmpty()) {
                throw new BusinessException("订单已全额退款，无需重复退款");
            }
        } else {
            // 部分退款
            for (RefundDetail item : refundDetailList) {
                if (item == null || item.getOrderDetailId() == null) {
                    throw new BusinessException("退款明细不合法");
                }
                OrderDetail d = detailMap.get(item.getOrderDetailId());
                if (d == null) {
                    throw new BusinessException("退款明细不属于该订单");
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new BusinessException("退款数量不合法");
                }
                int remaining = d.getQuantity() - refundQuantity(d);
                if (item.getQuantity() > remaining) {
                    String name = d.getProductName() != null ? d.getProductName() : "商品";
                    throw new BusinessException("「" + name + "」无法退款，最多可退 " + remaining + " 件");
                }
                refundDetails.add(item);
            }
        }

        // 3. 计算本次退款金额
        BigDecimal calcRefundAmount = BigDecimal.ZERO;
        Map<Integer, Integer> refundQtyMap = new HashMap<>(); // 本次要退数量
        for (RefundDetail item : refundDetails) {
            OrderDetail d = detailMap.get(item.getOrderDetailId());
            // 商品单价 = 明细小计 / 数量（保留两位小数）
            BigDecimal unitPrice = d.getCost()
                    .divide(BigDecimal.valueOf(d.getQuantity()), 2, RoundingMode.HALF_UP);
            calcRefundAmount = calcRefundAmount.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            refundQtyMap.merge(d.getId(), item.getQuantity(), Integer::sum);
        }
        // 退款后是否每条明细均已退完（运费/单价取整尾差在最后一次退款一并退回）
        boolean isAllFullyRefund = true;
        for (OrderDetail d : details) {
            int after = refundQuantity(d) + refundQtyMap.getOrDefault(d.getId(), 0);
            if (after < d.getQuantity()) { // 历史已退 + 本次要退 < 购买数量
                isAllFullyRefund = false;
                break;
            }
        }
        BigDecimal alreadyRefund = order.getRefundAmount() != null
                ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal refundAmount;
        if (isAllFullyRefund) {
            // 最后一次退款：退剩余全部金额（含运费及单价取整尾差），保证累计退款=订单总额
            refundAmount = order.getCost().subtract(alreadyRefund);
        } else {
            refundAmount = calcRefundAmount;
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("退款金额必须大于0");
        }
        if (alreadyRefund.add(refundAmount).compareTo(order.getCost()) > 0) {
            throw new BusinessException("累计退款金额不能超过订单总额");
        }

        // 4. 商户退款单号：支付宝 out_request_no / 微信 out_refund_no（多次部分退款 - 各个单号）
        String refundNo = "ERF" + order.getId() + "-" + System.currentTimeMillis();

        // 5. 调第三方退款 API（事务外调用：调第三方退款（成功） → 事务落库）
        Integer payType = order.getPayType();
        switch (payType) {
            // 支付宝：（trade_no）
            case 1: {
                Map<String, Object> r = alipayService.refund(
                        order.getTransactionId(), refundAmount, refundNo, reason);
                if (!Boolean.TRUE.equals(r.get("success"))) {
                    Object sub = r.get("subMsg");
                    throw new BusinessException("支付宝退款失败：" + (sub != null ? sub : r.get("msg")));
                }
                break;
            }
            // 微信：（transaction_id）
            case 2: {
                Map<String, Object> r = wechatPayService.refund(
                        order.getTransactionId(), refundAmount, order.getCost(), refundNo, reason);
                if (!Boolean.TRUE.equals(r.get("success"))) {
                    throw new BusinessException("微信退款失败：" + r.get("msg"));
                }
                break;
            }
            default:
                throw new BusinessException("不支持的支付方式，订单支付方式未知，无法退款");
        }

        // 6. 第三方退款成功，事务 - 落库（退款记录、明细已退数量、订单累计退款金额/状态、库存回退）
        orderService.refund(id, refundDetails, refundAmount, refundNo, reason);
    }

    /**
     * 明细已退数量
     */
    private int refundQuantity(OrderDetail d) {
        return d.getRefundQuantity() != null ? d.getRefundQuantity() : 0;
    }
}
