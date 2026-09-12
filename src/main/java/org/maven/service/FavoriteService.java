package org.maven.service;

import org.maven.cache.Favorite;

import java.util.List;

/**
 * 收藏夹业务接口
 *
 * 存储：Redis List（key = "favorite:{userId}"）
 * 规则：最多保存 6 个，先进先出
 */
public interface FavoriteService {

    /**
     * 查询当前用户收藏列表（按收藏时间倒序，最新收藏在前）
     */
    List<Favorite> list(Integer userId);

    /**
     * 添加收藏（已存在则移到最前；超出容量时移除最早收藏）
     *
     * @param userId    当前登录用户ID
     * @param productId 商品ID
     * @return 收藏列表
     */
    List<Favorite> add(Integer userId, Integer productId);

    /**
     * 移除单项收藏
     *
     * @return 收藏列表
     */
    List<Favorite> remove(Integer userId, Integer productId);

    /**
     * 清空收藏
     *
     * @return 空列表
     */
    List<Favorite> clear(Integer userId);
}
