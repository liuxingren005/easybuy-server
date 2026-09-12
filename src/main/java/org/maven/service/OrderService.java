package org.maven.service;

import org.maven.entity.Order;
import org.maven.request.RefundDetail;
import org.maven.entity.RefundRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单业务接口
 */
public interface OrderService {

    /**
     * 管理员分页条件查询全部订单
     */
    List<Order> findPage(String loginName, String serialNumber, Integer status,
                         String startTime, String endTime,
                         Integer pageNum, Integer pageSize);

    /**
     * 用户分页条件查询自己订单
     */
    List<Order> findPageByUserId(Integer userId, String serialNumber, Integer status,
                                 String startTime, String endTime,
                                 Integer pageNum, Integer pageSize);

    /**
     * 根据ID查询
     */
    Order findById(Integer id);

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> findByUserId(Integer userId);

    /**
     * 新增订单
     */
    void add(Order order);

    /**
     * 修改
     */
    void modify(Order order);

    /**
     * 支付订单（仅待付款状态支付）
     */
    void payOrder(Integer id);

    /**
     * 支付订单（指定支付方式，记录支付时间和交易号）
     * @param id 订单ID
     * @param payType 支付方式（1:支付宝 2:微信）
     * @param transactionId 第三方交易号
     */
    void payOrder(Integer id, Integer payType, String transactionId);

    /**
     * 退款 已付款(2)/部分退款(2)、存在第三方交易号与支付方式
     *
     * @return 明细订单
     */
    Order getOrderForRefund(Integer id);

    /**
     * 退款落库（第三方退款成功后调用，事务执行）
     * 累加明细已退数量与订单累计退款金额，部分退款2，全额退款4
     *
     * @param orderId      订单ID
     * @param items        本次退款明细
     * @param refundAmount 本次退款金额
     * @param refundNo     商户退款单号
     * @param reason       退款原因
     */
    void refund(Integer orderId, List<RefundDetail> items, BigDecimal refundAmount,
                String refundNo, String reason);

    /**
     * 查询订单退款（校验归属）
     */
    List<RefundRecord> findRefundsByOrderId(Integer orderId);

    /**
     * 关闭订单（仅待付款状态关闭）
     */
    void closeOrder(Integer id);

    /**
     * 关闭订单（指定关闭原因）
     */
    void closeOrder(Integer id, String reason);

    /**
     * 自动关闭订单（Redis 过期回调）
     * @param id 订单ID
     * @param reason 关闭原因
     */
    void autoCloseOrder(Integer id, String reason);

    /**
     * 根据订单号查询订单
     */
    Order findBySerialNumber(String serialNumber);

    /**
     * 删除订单（逻辑删除，仅已关闭状态删除）
     */
    void removeById(Integer id);

    /**
     * 根据商户订单号支付订单
     * 商户订单号格式：EB{orderId}{timestamp}
     *
     * @param outTradeNo    商户订单号
     * @param payType       支付方式（1:支付宝 2:微信）
     * @param transactionId 第三方交易号
     */
    void payOrderByOutTradeNo(String outTradeNo, Integer payType, String transactionId);
}
