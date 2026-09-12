package org.maven.request;

import lombok.Data;

/**
 * 退款明细（部分退款请求体）
 */
@Data
public class RefundDetail {

    /**
     * 订单明细ID（easybuy_order_detail.id）
     */
    private Integer orderDetailId;

    /**
     * 本次退款数量
     */
    private Integer quantity;
}
