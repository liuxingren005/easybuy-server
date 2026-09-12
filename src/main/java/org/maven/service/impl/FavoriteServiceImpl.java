package org.maven.service.impl;

import lombok.RequiredArgsConstructor;
import org.maven.cache.Favorite;
import org.maven.entity.Product;
import org.maven.exception.BusinessException;
import org.maven.service.FavoriteService;
import org.maven.service.ProductService;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 收藏夹业务实现类（Redis List 存储）
 *
 * 结构：key = "favorite:{userId}"，value = FavoriteItem 快照列表
 * 规则：LPUSH 新收藏到头部
 * 注：（名称/图片/价格）读时回源数据库刷新
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    /**
     * Redis key 前缀 favorite:{userId}
     */
    private static final String KEY_PREFIX = "favorite:";

    /**
     * 收藏夹容量
     */
    private static final int MAX_SIZE = 6;

    /**
     * 收藏夹过期时间（天），写操作续期
     */
    private static final long EXPIRE_DAYS = 7;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ProductService productService;

    /**
     * 拼接收藏夹 Redis key
     */
    private String key(Integer userId) {
        return KEY_PREFIX + userId;
    }

    @Override
    public List<Favorite> list(Integer userId) {
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        List<Object> raw = listOps.range(key(userId), 0, -1);
        List<Favorite> list = new ArrayList<>();
        if (raw == null) {
            return list;
        }
        for (Object obj : raw) {
            Favorite item = (Favorite) obj;
            // 读时实时回源：名称/图片/价格，避免 Redis 快照过期
            Product product = productService.findById(item.getId());
            if (product == null) {
                // 商品已下架/删除：标记失效，保留条目
                item.setInvalid(true);
                listOps.set(key(userId), list.indexOf(item), item);
                list.add(item);
                continue;
            }
            // 商品有效：清除失效标记并刷新数据
            item.setInvalid(false);
            item.setName(product.getName());
            item.setFileName(product.getFileName());
            item.setPrice(product.getPrice());
            list.add(item);
        }
        return list;
    }

    @Override
    public List<Favorite> add(Integer userId, Integer productId) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        Product product = productService.findById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在或已下架");
        }

        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        String key = key(userId);

        // 已存在则先移除（避免重复），再 LPUSH 到头部
        List<Object> existing = listOps.range(key, 0, -1);
        if (existing != null) {
            for (Object obj : existing) {
                Favorite item = (Favorite) obj;
                if (item.getId().equals(productId)) {
                    listOps.remove(key, 1, item);
                    break;
                }
            }
        }

        // 构建快照并写入头部
        Favorite item = new Favorite();
        item.setId(product.getId());
        item.setName(product.getName());
        item.setFileName(product.getFileName());
        item.setPrice(product.getPrice());
        item.setInvalid(false);
        listOps.leftPush(key, item);

        // 超出最大容量：移除最早收藏（列表尾部），先进先出
        Long size = listOps.size(key);
        if (size != null && size > MAX_SIZE) {
            listOps.rightPop(key); // 尾部淘汰
        }

        // 续期：写操作重置过期时间
        redisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);
        return list(userId);
    }

    @Override
    public List<Favorite> remove(Integer userId, Integer productId) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        String key = key(userId);
        List<Object> existing = listOps.range(key, 0, -1);
        if (existing != null) {
            for (Object obj : existing) {
                Favorite item = (Favorite) obj;
                if (item.getId().equals(productId)) {
                    listOps.remove(key, 1, item);
                    break;
                }
            }
        }
        return list(userId);
    }

    @Override
    public List<Favorite> clear(Integer userId) {
        redisTemplate.delete(key(userId));
        return new ArrayList<>();
    }
}
