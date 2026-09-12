package org.maven.service.impl;

import lombok.RequiredArgsConstructor;
import org.maven.cache.Cart;
import org.maven.entity.Product;
import org.maven.exception.BusinessException;
import org.maven.service.CartService;
import org.maven.service.ProductService;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 购物车业务实现类（Redis Hash 存储）
 *
 * 结构：key = "cart:{userId}"，field = 商品ID，value = CartItem 快照
 * 注：（名称/图片/价格/库存）读时回源数据库刷新
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    /**
     * Redis key 前缀 cart:{userId}
     */
    private static final String KEY_PREFIX = "cart:";

    /**
     * 购物车过期时间（天），写操作续期
     */
    private static final long EXPIRE_DAYS = 7;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ProductService productService;

    /**
     * 拼接购物车 Redis key
     */
    private String key(Integer userId) {
        return KEY_PREFIX + userId;
    }

    @Override
    public List<Cart> list(Integer userId) {
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        Map<Object, Object> entries = hashOps.entries(key(userId));
        List<Cart> list = new ArrayList<>();
        // 集合 → 列表
        /* for (Object value : entries.values()) {
            list.add((CartItem) value);
        } */
       for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Cart item = (Cart) entry.getValue();
            // 读时实时回源：名称/图片/价格/库存，避免 Redis 快照过期
            Product product = productService.findById(item.getId());
            if (product == null) {
                // 商品已下架/删除：静默移除该条目
                /* hashOps.delete(key(userId), entry.getKey());
                continue; */
                // 商品已下架/删除：标记失效，保留条目
                item.setInvalid(true);
                hashOps.put(key(userId), entry.getKey(), item);
                list.add(item);
                continue;
            }
            // 商品有效：清除失效标记并刷新数据
            item.setInvalid(false);
            item.setName(product.getName());
            item.setFileName(product.getFileName());
            item.setPrice(product.getPrice());
            item.setStock(product.getStock());
            // 回写最新数据，清理陈旧快照
            hashOps.put(key(userId), entry.getKey(), item);
            list.add(item);
        }
        // 按加入时间倒序（最新加入在最前）
        list.sort(Comparator.comparingLong((Cart i) -> i.getAddTime()).reversed());
        return list;
    }

    @Override
    public List<Cart> add(Integer userId, Integer productId, Integer quantity) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        if (quantity == null || quantity < 1) {
            throw new BusinessException("购买数量不合法");
        }
        // 商品表
        Product product = productService.findById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (product.getStock() != null && quantity > product.getStock()) {
            throw new BusinessException("库存不足，请重新选择");
        }

        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        String field = String.valueOf(productId);
        Object existing = hashOps.get(key(userId), field);
        Cart item;
        if (existing != null) {
            // 已存在：数量累加
            item = (Cart) existing;
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            item = new Cart();
            item.setId(product.getId());
            item.setName(product.getName());
            item.setFileName(product.getFileName());
            item.setPrice(product.getPrice());
            item.setStock(product.getStock());
            item.setQuantity(quantity);
            item.setAddTime(System.currentTimeMillis());
        }
        // 仅校验库存
        if (product.getStock() != null && item.getQuantity() > product.getStock()) {
            throw new BusinessException("库存不足，请重新选择");
        }
        hashOps.put(key(userId), field, item);
        // 续期：写操作重置过期时间为 7 天
        // 购物车 7 天不活跃自动过期删除，释放内存
        redisTemplate.expire(key(userId), EXPIRE_DAYS, TimeUnit.DAYS);
        return list(userId);
    }

    @Override
    public List<Cart> updateQuantity(Integer userId, Integer productId, Integer quantity) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        if (quantity == null || quantity < 1) {
            throw new BusinessException("购买数量不合法");
        }
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        String field = String.valueOf(productId);
        Object existing = hashOps.get(key(userId), field);
        if (existing == null) {
            throw new BusinessException("该商品不在购物车中");
        }
        Cart item = (Cart) existing;
        // 数量上限（回源库存）
        Product product = productService.findById(productId);
        if (product == null) {
            // 商品已下架/删除：静默移除该条目
            /* hashOps.delete(key(userId), field);
            throw new BusinessException("商品不存在或已下架"); */
            // 商品已下架/删除：标记失效，保留条目
            item.setInvalid(true);
            hashOps.put(key(userId), field, item);
            throw new BusinessException("商品不存在或已下架");
        }
        if (product.getStock() != null && quantity > product.getStock()) {
            throw new BusinessException("库存不足，请重新选择");
        }
        item.setQuantity(quantity);
        hashOps.put(key(userId), field, item);
        redisTemplate.expire(key(userId), EXPIRE_DAYS, TimeUnit.DAYS);
        return list(userId);
    }

    @Override
    public List<Cart> remove(Integer userId, Integer productId) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        redisTemplate.opsForHash().delete(key(userId), String.valueOf(productId));
        return list(userId);
    }

    @Override
    public List<Cart> clear(Integer userId) {
        redisTemplate.delete(key(userId));
        return new ArrayList<>();
    }
}
