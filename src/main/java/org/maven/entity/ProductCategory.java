package org.maven.entity;

import lombok.Data;

import java.util.List;

/**
 * 商品分类实体类
 */
@Data
public class ProductCategory {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 父级目录id
     */
    private Integer parentId;

    /**
     * 级别(1:一级 2:二级 3:三级)
     */
    private Integer type;

    /**
     * 图标
     */
    private String iconClass;

    /**
     * 是否删除（0:未删除 1:已删除）
     */
    private Integer isDelete;

    /**
     * 子分类列表（关联查询）
     */
    private List<ProductCategory> childCategory;
}
