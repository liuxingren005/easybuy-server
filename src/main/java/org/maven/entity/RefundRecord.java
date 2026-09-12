package org.maven.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单退款记录实体类
 * 每次退款（全额/部分）成功后写入一条，支持多次部分退款
 */
@Data
public class RefundRecord {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 订单主键
     */
    private Integer orderId;

    /**
     * 商户退款单号（支付宝 out_request_no / 微信 out_refund_no）
     */
    private String refundNo;

    /**
     * 本次退款金额
     */
    private BigDecimal amount;

    /**
     * 退款原因
     */
    private String reason;

    /**
     * 支付方式（1:支付宝 2:微信支付）
     */
    private Integer payType;

    /**
     * 第三方交易号快照（支付宝 trade_no / 微信 transaction_id）
     */
    private String transactionId;

    /**
     * 退款时间
     */
    private Date createTime;
}
