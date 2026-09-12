package org.maven.cache;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收藏条目（Redis 缓存对象）
 */
@Data
public class Favorite {

    /**
     * 商品ID
     */
    private Integer id;

    /**
     * 商品名称（回源）
     */
    private String name;

    /**
     * 商品图片文件名（回源）
     */
    private String fileName;

    /**
     * 商品单价（回源）
     */
    private BigDecimal price;

    /**
     * 失效标记（商品已下架/删除）
     */
    private Boolean invalid;
}
