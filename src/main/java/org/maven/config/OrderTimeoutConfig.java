package org.maven.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maven.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Key 过期事件：监听 Redis __keyevent@0__:expired 频道
 *
 * 用途：监听订单支付超时 key（order:pay:timeout:{orderId}）的过期事件，15 分钟未支付的订单自动关闭
 *
 * 前置条件：Redis 服务器开启 keyspace notifications：
 *   redis-cli CONFIG SET notify-keyspace-events Ex
 *   （E = 键事件通知，x = 过期事件）
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderTimeoutConfig {

    /**
     * 订单支付超时 key 前缀
     */
    private static final String PAY_TIMEOUT_PREFIX = "order:pay:timeout:";

    /**
     * Redis 过期事件频道（db 0）
     */
    private static final String EXPIRED_TOPIC = "__keyevent@0__:expired";

    /**
     * 注册消息监听容器，订阅 Redis 键过期事件
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            OrderTimeoutListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new PatternTopic(EXPIRED_TOPIC));
        return container;
    }

    /**
     * 订单支付超时监听器：匹配 order:pay:timeout: 前缀的过期 key，
     * 解析 orderId 后调用 autoCloseOrder 自动关闭
     */
    @Component
    public static class OrderTimeoutListener implements MessageListener {

        private final OrderService orderService;

        public OrderTimeoutListener(OrderService orderService) {
            this.orderService = orderService;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);
            if (!expiredKey.startsWith(PAY_TIMEOUT_PREFIX)) {
                return;
            }
            String orderIdStr = expiredKey.substring(PAY_TIMEOUT_PREFIX.length());
            try {
                Integer orderId = Integer.parseInt(orderIdStr);
                log.info("订单支付超时，自动关闭：orderId={}", orderId);
                orderService.autoCloseOrder(orderId, "支付超时自动关闭");
            } catch (NumberFormatException e) {
                log.warn("无法解析订单ID：key={}", expiredKey);
            } catch (Exception e) {
                log.error("订单超时自动关闭失败：orderId={}，原因：{}",
                        orderIdStr, e.getMessage());
            }
        }
    }
}
