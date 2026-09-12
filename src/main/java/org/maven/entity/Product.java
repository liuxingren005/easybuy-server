package org.maven.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品实体类
 */
@Data
public class Product {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 预出库库存（已提交订单但未付款）
     */
    private Integer preStock;

    /**
     * 一级分类ID
     */
    private Integer categoryLevel1Id;

    /**
     * 二级分类ID
     */
    private Integer categoryLevel2Id;

    /**
     * 三级分类ID
     */
    private Integer categoryLevel3Id;

    /**
     * 图片文件名
     */
    private String fileName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除(1:删除 0:未删除)
     */
    private Integer isDelete;
}
