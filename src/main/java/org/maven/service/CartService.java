package org.maven.service;

import org.maven.cache.Cart;

import java.util.List;

/**
 * 购物车业务接口
 *
 * 存储：Redis Hash（key = "cart:{userId}"）
 */
public interface CartService {

    /**
     * 查询当前用户购物车列表（按加入时间倒序）
     */
    List<Cart> list(Integer userId);

    /**
     * 加入购物车（已存在则数量累加）
     *
     * @param userId    当前登录用户ID
     * @param productId 商品ID
     * @param quantity  数量（>=1）
     * @return 购物车列表
     */
    List<Cart> add(Integer userId, Integer productId, Integer quantity);

    /**
     * 修改购买数量
     *
     * @return 全量购物车列表
     */
    List<Cart> updateQuantity(Integer userId, Integer productId, Integer quantity);

    /**
     * 移除单项
     *
     * @return 全量购物车列表
     */
    List<Cart> remove(Integer userId, Integer productId);

    /**
     * 清空购物车
     *
     * @return 空列表
     */
    List<Cart> clear(Integer userId);
}
