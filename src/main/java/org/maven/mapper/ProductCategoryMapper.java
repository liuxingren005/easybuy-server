package org.maven.mapper;

import org.maven.entity.ProductCategory;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 商品分类数据访问接口
 */
public interface ProductCategoryMapper {

    /**
     * 查询所有分类
     */
    List<ProductCategory> selectAll();

    /**
     * 根据父级ID查询子分类
     */
    List<ProductCategory> selectByParentId(Integer parentId);

    /**
     * 根据类型查询分类
     */
    List<ProductCategory> selectByType(Integer type);

    /**
     * 根据ID查询
     */
    ProductCategory selectById(Integer id);

    /**
     * 分页条件查询
     */
    List<ProductCategory> selectPage(@Param("name") String name,
                                     @Param("type") Integer type);

    /**
     * 根据名称和父级ID查询
     */
    ProductCategory selectByNameAndParentId(@Param("name") String name,
                                            @Param("parentId") Integer parentId);

    /**
     * 子分类统计
     */
    int countByParentId(Integer parentId);

    /**
     * 商品引用统计
     */
    int countProductByCategoryId(Integer categoryId);

    /**
     * 新增
     */
    int insert(ProductCategory productCategory);

    /**
     * 修改
     */
    int update(ProductCategory productCategory);

    /**
     * 删除
     */
    int deleteById(Integer id);
}
