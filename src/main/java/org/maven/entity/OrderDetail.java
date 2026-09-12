package org.maven.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细实体类
 */
@Data
public class OrderDetail {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 订单主键
     */
    private Integer orderId;

    /**
     * 商品主键
     */
    private Integer productId;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 消费
     */
    private BigDecimal cost;

    /**
     * 已退款数量
     */
    private Integer refundQuantity;

    /**
     * 商品名称（关联查询 easybuy_product）
     */
    private String productName;
}
