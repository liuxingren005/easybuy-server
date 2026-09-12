package org.maven.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单实体类
 */
@Data
public class Order {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 用户主键
     */
    private Integer userId;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 用户地址
     */
    private String userAddress;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 总消费
     */
    private BigDecimal cost;

    /**
     * 累计退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 订单号
     */
    private String serialNumber;

    /**
     * 订单状态（1:待付款 2:已付款 3:已关闭 4:已退款）
     * 部分退款2（已付款），累计退款金额达订单总额后4（已退款）
     */
    private Integer status;

    /**
     * 是否删除（1:已删除 0:未删除）
     */
    private Integer isDelete;

    /**
     * 支付方式（1:支付宝 2:微信支付）
     */
    private Integer payType;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 第三方交易号（支付宝/微信交易号）
     */
    private String transactionId;

    /**
     * 退款时间
     */
    private Date refundTime;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 关闭时间
     */
    private Date closeTime;

    /**
     * 关闭原因
     */
    private String closeReason;

    /**
     * 订单明细列表（关联查询）
     */
    private List<OrderDetail> orderDetailList;
}
