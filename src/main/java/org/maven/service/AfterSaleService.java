package org.maven.service;

import org.maven.request.RefundDetail;

import java.util.List;

/**
 * 订单退款服务接口
 * 协调第三方退款 API 调用与订单状态更新
 * 支持整单全额退款与按明细多次部分退款
 */
public interface AfterSaleService {

    /**
     * 整单全额退款（退所有未退商品）
     *
     * @param id 订单ID
     */
    void refundOrder(Integer id);

    /**
     * 整单全额退款（指定退款原因）
     *
     * @param id     订单ID
     * @param reason 退款原因（可选）
     */
    void refundOrder(Integer id, String reason);

    /**
     * 退款（按明细部分退款）
     *
     * @param id     订单ID
     * @param items  退款明细（订单明细ID + 退款数量）
     * @param reason 退款原因（可选）
     */
    void refundOrder(Integer id, List<RefundDetail> items, String reason);
}
