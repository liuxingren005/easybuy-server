package org.maven.mapper;

import org.maven.entity.RefundRecord;

import java.util.List;

/**
 * 订单退款记录数据访问接口
 */
public interface RefundMapper {

    /**
     * 新增退款记录
     */
    int insert(RefundRecord refundRecord);

    /**
     * 根据订单ID查询退款记录（按时间正序）
     */
    List<RefundRecord> selectByOrderId(Integer orderId);
}
