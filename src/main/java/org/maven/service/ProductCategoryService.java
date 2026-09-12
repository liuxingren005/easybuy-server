package org.maven.service;

import org.maven.entity.ProductCategory;

import java.util.List;

/**
 * 商品分类业务接口
 */
public interface ProductCategoryService {

    /**
     * 查询所有分类
     */
    List<ProductCategory> findAll();

    /**
     * 查询一级分类及其子分类（树形结构）
     */
    List<ProductCategory> findCategoryTree();

    /**
     * 根据父级ID查询子分类
     */
    List<ProductCategory> findByParentId(Integer parentId);

    /**
     * 根据类型查询分类
     */
    List<ProductCategory> findByType(Integer type);

    /**
     * 根据ID查询
     */
    ProductCategory findById(Integer id);

    /**
     * 新增
     */
    void add(ProductCategory productCategory);

    /**
     * 修改
     */
    void modify(ProductCategory productCategory);

    /**
     * 删除
     */
    void removeById(Integer id);

    /**
     * 分页条件查询
     */
    List<ProductCategory> findPage(String name, Integer type,
                                   Integer pageNum, Integer pageSize);
}
