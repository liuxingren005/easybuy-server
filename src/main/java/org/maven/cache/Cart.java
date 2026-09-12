package org.maven.cache;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车条目（Redis 缓存对象）- Hash字段级操作/JSON整体读写
 */
@Data
public class Cart {

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
     * 库存（回源）
     */
    private Integer stock;

    /**
     * 失效标记
     */
    private Boolean invalid;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 时间戳（毫秒）
     * 列表排序
     */
    private Long addTime;
}
