package org.maven.service;

import org.maven.entity.Product;

import java.util.List;

/**
 * 商品业务接口
 */
public interface ProductService {

    /**
     * 分页条件查询
     */
    List<Product> findPage(String name, String startTime, String endTime,
                           Integer categoryLevel1Id, Integer categoryLevel2Id, Integer categoryLevel3Id,
                           Integer pageNum, Integer pageSize);

    /**
     * 查询全部商品
     */
    List<Product> findAll();

    /**
     * 根据一级分类ID查询商品
     */
    List<Product> findByCategoryLevel1Id(Integer categoryLevel1Id, Integer limit);

    /**
     * 根据二级分类ID查询商品
     */
    List<Product> findByCategoryLevel2Id(Integer categoryLevel2Id, Integer limit);

    /**
     * 根据三级分类ID查询商品
     */
    List<Product> findByCategoryLevel3Id(Integer categoryLevel3Id, Integer limit);

    /**
     * 根据ID查询
     */
    Product findById(Integer id);

    /**
     * 前台-热门推荐（随机抽取商品）
     */
    List<Product> findHot(Integer limit);

    /**
     * 新增
     */
    void add(Product product);

    /**
     * 修改
     */
    void modify(Product product);

    /**
     * 逻辑删除
     */
    void removeById(Integer id);
}
