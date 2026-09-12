package org.maven.mapper;

import org.maven.entity.OrderDetail;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 订单明细数据访问接口
 */
public interface OrderDetailMapper {

    /**
     * 根据订单ID查询明细列表（关联商品名称）
     */
    List<OrderDetail> selectByOrderId(Integer orderId);

    /**
     * 根据ID查询
     */
    OrderDetail selectById(Integer id);

    /**
     * 新增
     */
    int insert(OrderDetail orderDetail);

    /**
     * 批量新增
     */
    int batchInsert(@Param("list") List<OrderDetail> list);

    /**
     * 退款：累加明细已退数量
     * 条件 剩余可退数量 >= 本次退款数量
     */
    int addRefundQuantity(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /**
     * 根据订单ID删除
     */
    int deleteByOrderId(Integer orderId);
}
