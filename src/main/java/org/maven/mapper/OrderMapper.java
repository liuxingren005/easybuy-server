package org.maven.mapper;

import org.apache.ibatis.annotations.Param;
import org.maven.entity.Order;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单数据访问接口
 */
public interface OrderMapper {

    /**
     * 管理员分页条件查询全部订单
     */
    List<Order> selectPage(@Param("loginName") String loginName,
                           @Param("serialNumber") String serialNumber,
                           @Param("status") Integer status,
                           @Param("startTime") String startTime,
                           @Param("endTime") String endTime);

    /**
     * 用户分页条件查询自己订单
     */
    List<Order> selectPageByUserId(@Param("userId") Integer userId,
                                   @Param("serialNumber") String serialNumber,
                                   @Param("status") Integer status,
                                   @Param("startTime") String startTime,
                                   @Param("endTime") String endTime);

    /**
     * 根据ID查询
     */
    Order selectById(Integer id);

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> selectByUserId(Integer userId);

    /**
     * 新增
     */
    int insert(Order order);

    /**
     * 修改
     */
    int update(Order order);

    /**
     * 修改订单状态
     */
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);


    /**
     * 支付订单：更新状态、支付方式、支付时间、交易号
     */
    int payOrder(@Param("id") Integer id,
                 @Param("payType") Integer payType,
                 @Param("payTime") Date payTime,
                 @Param("transactionId") String transactionId);

    /**
     * 关闭订单：更新状态、关闭时间、关闭原因
     */
    int closeOrder(@Param("id") Integer id,
                   @Param("closeTime") Date closeTime,
                   @Param("closeReason") String closeReason);

    /**
     * 退款落库：累加退款金额，部分退款2（已付款），全额退款4（已退款）
     * 仅已付款(2)订单退款
     */
    int refund(@Param("id") Integer id,
               @Param("refundAmount") BigDecimal refundAmount,
               @Param("status") Integer status,
               @Param("refundTime") Date refundTime,
               @Param("refundReason") String refundReason);

    /**
     * 根据订单号查询订单
     */
    Order selectBySerialNumber(@Param("serialNumber") String serialNumber);

    /**
     * 删除
     */
    int deleteById(Integer id);
}
