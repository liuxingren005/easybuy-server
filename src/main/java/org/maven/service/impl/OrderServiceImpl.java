package org.maven.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maven.common.Role;
import org.maven.entity.Order;
import org.maven.entity.OrderDetail;
import org.maven.entity.Product;
import org.maven.request.RefundDetail;
import org.maven.entity.RefundRecord;
import org.maven.exception.BusinessException;
import org.maven.mapper.OrderDetailMapper;
import org.maven.mapper.OrderMapper;
import org.maven.mapper.ProductMapper;
import org.maven.mapper.RefundMapper;
import org.maven.security.UserContext;
import org.maven.service.OrderService;
import org.maven.service.ProductEsService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final ProductMapper productMapper;
    private final RefundMapper refundMapper;
    private final ProductEsService productEsService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 订单支付超时 Redis key 前缀 order:pay:timeout:{orderId}
     */
    private static final String PAY_TIMEOUT_KEY_PREFIX = "order:pay:timeout:";

    /**
     * 订单支付超时时间（分钟），15 分钟未支付自动关闭
     */
    private static final long PAY_TIMEOUT_MINUTES = 15;

    /**
     * 订单归属：管理员操作所有订单，普通用户仅操作自己订单
     */
    private void checkOrderOwner(Order order) {
        if (UserContext.get() == null) {
            throw new BusinessException("未登录");
        }
        boolean isAdmin = Role.ADMIN.getLevel() == UserContext.get().getType();
        if (!isAdmin && !order.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权操作他人订单");
        }
    }

    @Override
    public List<Order> findPage(String loginName, String serialNumber, Integer status,
                                String startTime, String endTime,
                                Integer pageNum, Integer pageSize) {
        try (Page<?> page = PageHelper.startPage(pageNum, pageSize)) {
            return orderMapper.selectPage(loginName, serialNumber, status, startTime, endTime);
        }
    }

    @Override
    public List<Order> findPageByUserId(Integer userId, String serialNumber, Integer status,
                                        String startTime, String endTime,
                                        Integer pageNum, Integer pageSize) {
        try (Page<?> page = PageHelper.startPage(pageNum, pageSize)) {
            return orderMapper.selectPageByUserId(userId, serialNumber, status, startTime, endTime);
        }
    }

    @Override
    public Order findById(Integer id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            List<OrderDetail> detailList = orderDetailMapper.selectByOrderId(id);
            order.setOrderDetailList(detailList);
        }
        return order;
    }

    @Override
    public List<Order> findByUserId(Integer userId) {
        List<Order> orderList = orderMapper.selectByUserId(userId);
        for (Order order : orderList) {
            List<OrderDetail> detailList = orderDetailMapper.selectByOrderId(order.getId());
            order.setOrderDetailList(detailList);
        }
        return orderList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Order order) {
        // 生成订单号
        order.setSerialNumber(UUID.randomUUID().toString().replace("-", "").toUpperCase());
        order.setCreateTime(new Date());
        // 插入主单
        if (orderMapper.insert(order) == 0) {
            throw new BusinessException("下单失败");
        }
        if (order.getOrderDetailList() != null && !order.getOrderDetailList().isEmpty()) {
            // 遍历明细
            for (OrderDetail detail : order.getOrderDetailList()) {
                detail.setOrderId(order.getId());
                // 提交订单：每条明细库存扣减至预出库（并发安全，WHERE stock >= quantity）
                int rows = productMapper.deductStock(detail.getProductId(), detail.getQuantity());
                if (rows == 0) {
                    Product product = productMapper.selectById(detail.getProductId());
                    String name = product != null ? product.getName() : "商品";
                    throw new BusinessException("「" + name + "」库存不足，无法下单");
                }
                // 库存变化同步 ES（搜索列表页库存实时）
                syncProductEs(detail.getProductId());
            }
            orderDetailMapper.batchInsert(order.getOrderDetailList());
        }
        // 设置支付超时 key：15 分钟后过期，触发 Redis 过期回调自动关闭订单
        redisTemplate.opsForValue().set(
                PAY_TIMEOUT_KEY_PREFIX + order.getId(), order.getId(), PAY_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        // 设置支付超时 key：15 分钟后过期，触发 Redis 过期回调自动关闭订单，事务提交后再写入
        /*deferAction(() -> redisTemplate.opsForValue().set(
                PAY_TIMEOUT_KEY_PREFIX + order.getId(), order.getId(), PAY_TIMEOUT_MINUTES, TimeUnit.MINUTES));*/
    }

    @Override
    public void modify(Order order) {
        if (orderMapper.update(order) == 0) {
            throw new BusinessException("修改订单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(Integer id) {
        closeOrder(id, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(Integer id, String reason) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        checkOrderOwner(order);
        if (order.getStatus() != 1) {
            throw new BusinessException("仅待付款订单关闭");
        }
        if (orderMapper.closeOrder(id, new Date(), reason) == 0) {
            throw new BusinessException("关闭订单失败");
        }
        // 取消订单：预出库回退至库存（超时关闭 / 手动取消）
        restorePreStockByOrderId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoCloseOrder(Integer id, String reason) {
        // Redis 过期回调
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return; // 订单不存在，静默
        }
        if (order.getStatus() != 1) {
            return;
        }
        orderMapper.closeOrder(id, new Date(), reason); // 状态 → 3
        // 超时自动关闭：预出库回退至库存
        restorePreStockByOrderId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Integer id) {
        payOrder(id, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Integer id, Integer payType, String transactionId) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        checkOrderOwner(order);
        // 幂等：订单已付款（第三方异步回调/同步查询可能已先行更新）
        if (order.getStatus() == 2) {
            log.info("订单已支付，重复支付请求幂等忽略：orderId={}", id);
            return;
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("仅待付款订单支付");
        }
        if (orderMapper.payOrder(id, payType, new Date(), transactionId) == 0) { // 状态 → 2 - 支付方式、交易号、pay_time、status=2
            throw new BusinessException("支付订单失败");
        }
        // 支付成功：确认预出库
        confirmPreStockByOrderId(id);
        // 支付成功，事务提交后再取消超时自动关闭
        /*redisTemplate.delete(PAY_TIMEOUT_KEY_PREFIX + id);*/
        deferAction(() ->
                redisTemplate.delete(PAY_TIMEOUT_KEY_PREFIX + id));
    }

    @Override
    public Order getOrderForRefund(Integer id) {
        Order order = findById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        checkOrderOwner(order);
        // 已付款(2)；部分退款(2)，全部退款(4)
        if (order.getStatus() != 2) {
            throw new BusinessException("仅已付款订单可退款");
        }
        if (order.getTransactionId() == null || order.getTransactionId().isEmpty()) {
            throw new BusinessException("订单缺少第三方交易号，无法发起退款");
        }
        if (order.getPayType() == null) {
            throw new BusinessException("订单支付方式未知，无法退款");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Integer orderId, List<RefundDetail> items, BigDecimal refundAmount,
                       String refundNo, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        List<OrderDetail> details = orderDetailMapper.selectByOrderId(orderId);
        Map<Integer, OrderDetail> detailMap = details.stream()
                .collect(Collectors.toMap(OrderDetail::getId, Function.identity()));

        // 1. 明细累加已退数量，并按本次退款数量回退库存
        for (RefundDetail item : items) {
            OrderDetail detail = detailMap.get(item.getOrderDetailId());
            if (detail == null) {
                throw new BusinessException("退款明细不属于该订单");
            }
            if (orderDetailMapper.addRefundQuantity(item.getOrderDetailId(), item.getQuantity()) == 0) {
                String name = detail.getProductName() != null ? detail.getProductName() : "商品";
                throw new BusinessException("「" + name + "」可退数量不足，退款失败");
            }
            // 退款：库存回退（仅本次退款数量）
            productMapper.restoreStock(detail.getProductId(), item.getQuantity());
            // 库存变化同步 ES（搜索列表页库存实时）
            syncProductEs(detail.getProductId());
        }

        // 2. 写退款记录
        RefundRecord record = new RefundRecord();
        record.setOrderId(orderId);
        record.setRefundNo(refundNo);
        record.setAmount(refundAmount);
        record.setReason(reason);
        record.setPayType(order.getPayType());
        record.setTransactionId(order.getTransactionId());
        record.setCreateTime(new Date());
        refundMapper.insert(record); // 写退款流水 refund_record（每次部分退款快照）

        // 3. 更新订单累计退款金额与状态：累计退款达订单总额→已退款(4)，否则保持已付款(2)
        BigDecimal alreadyRefunded = order.getRefundAmount() != null
                ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal newRefundAmount = alreadyRefunded.add(refundAmount);
        int newStatus = newRefundAmount.compareTo(order.getCost()) >= 0 ? 4 : 2;
        if (orderMapper.refund(orderId, newRefundAmount, newStatus, new Date(), reason) == 0) {
            throw new BusinessException("退款已发起，但订单状态更新失败，请人工核对");
        }
    }

    @Override
    public List<RefundRecord> findRefundsByOrderId(Integer orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        checkOrderOwner(order);
        return refundMapper.selectByOrderId(orderId);
    }

    @Override
    public Order findBySerialNumber(String serialNumber) {
        return orderMapper.selectBySerialNumber(serialNumber);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        checkOrderOwner(order);
        if (order.getStatus() != 3) {
            throw new BusinessException("仅已关闭订单删除");
        }
        if (orderMapper.deleteById(id) == 0) {
            throw new BusinessException("删除订单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrderByOutTradeNo(String outTradeNo, Integer payType, String transactionId) {
        // 商户订单号格式：EB{orderId}-{timestamp}（支付记录表）
        if (outTradeNo == null || !outTradeNo.startsWith("EB")) {
            throw new BusinessException("商户订单号格式不正确");
        }

        // 解析订单ID
        String remain = outTradeNo.substring(2);
        StringBuilder orderIdStr = new StringBuilder();
        for (int i = 0; i < remain.length(); i++) {
            if (Character.isDigit(remain.charAt(i))) {
                orderIdStr.append(remain.charAt(i));
            } else {
                break;
            }
        }

        if (orderIdStr.isEmpty()) {
            throw new BusinessException("商户订单号中未解析到订单ID");
        }

        Integer orderId = Integer.parseInt(orderIdStr.toString());

        // 异步回调无用户上下文
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 幂等：异步通知/同步跳转/前端轮询
        if (order.getStatus() == 2) {
            log.info("订单已支付，支付同步请求幂等忽略：orderId={}, outTradeNo={}", orderId, outTradeNo);
            return;
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("仅待付款订单支付");
        }
        if (orderMapper.payOrder(orderId, payType, new Date(), transactionId) == 0) {
            throw new BusinessException("支付订单失败");
        }
        // 支付成功：确认预出库
        confirmPreStockByOrderId(orderId);
        // 支付成功，事务提交后再取消超时自动关闭
        /*redisTemplate.delete(PAY_TIMEOUT_KEY_PREFIX + orderId);*/
        deferAction(() ->
                redisTemplate.delete(PAY_TIMEOUT_KEY_PREFIX + orderId));
    }

    /**
     * 确认预出库（付款成功后调用）
     */
    private void confirmPreStockByOrderId(Integer orderId) {
        List<OrderDetail> details = orderDetailMapper.selectByOrderId(orderId);
        if (details == null || details.isEmpty()) {
            return;
        }
        for (OrderDetail detail : details) {
            productMapper.confirmPreStock(detail.getProductId(), detail.getQuantity());
            // 库存变化同步 ES（搜索列表页库存实时）
            syncProductEs(detail.getProductId());
        }
    }

    /**
     * 预出库回退至库存（取消订单时调用）
     */
    private void restorePreStockByOrderId(Integer orderId) {
        List<OrderDetail> details = orderDetailMapper.selectByOrderId(orderId);
        if (details == null || details.isEmpty()) {
            return;
        }
        for (OrderDetail detail : details) {
            productMapper.restorePreStock(detail.getProductId(), detail.getQuantity());
            // 库存变化同步 ES（搜索列表页库存实时）
            syncProductEs(detail.getProductId());
        }
    }

    /**
     * 库存变化同步 ES（搜索列表页库存实时）
     */
    private void syncProductEs(Integer productId) {
        try {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                productEsService.sync(product);
            }
        } catch (Exception e) {
            log.warn("商品库存同步ES失败（productId={}）：{}",
                    productId, e.getMessage());
        }
    }

    /**
     * 事务同步
     */
    private void deferAction(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    });
        } else {
            action.run();
        }
    }
}
