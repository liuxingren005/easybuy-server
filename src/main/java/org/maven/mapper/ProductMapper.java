package org.maven.mapper;

import org.maven.entity.Product;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 商品数据访问接口
 */
public interface ProductMapper {

    /**
     * 分页条件查询
     */
    List<Product> selectPage(@Param("name") String name,
                             @Param("startTime") String startTime,
                             @Param("endTime") String endTime,
                             @Param("categoryLevel1Id") Integer categoryLevel1Id,
                             @Param("categoryLevel2Id") Integer categoryLevel2Id,
                             @Param("categoryLevel3Id") Integer categoryLevel3Id);

    /**
     * 查询全部商品
     */
    List<Product> selectAll();

    /**
     * 根据一级分类ID查询商品
     */
    List<Product> selectByCategoryLevel1Id(@Param("categoryLevel1Id") Integer categoryLevel1Id,
                                           @Param("limit") Integer limit);

    /**
     * 根据二级分类ID查询商品
     */
    List<Product> selectByCategoryLevel2Id(@Param("categoryLevel2Id") Integer categoryLevel2Id,
                                           @Param("limit") Integer limit);

    /**
     * 根据三级分类ID查询商品
     */
    List<Product> selectByCategoryLevel3Id(@Param("categoryLevel3Id") Integer categoryLevel3Id,
                                           @Param("limit") Integer limit);

    /**
     * 根据ID查询
     */
    Product selectById(Integer id);

    /**
     * 前台-热门推荐（随机抽取商品）
     */
    List<Product> selectHot(@Param("limit") Integer limit);

    /**
     * 新增
     */
    int insert(Product product);

    /**
     * 修改
     */
    int update(Product product);

    /**
     * 逻辑删除
     */
    int deleteById(Integer id);

    /**
     * 提交订单：库存扣减至预出库（stock -= quantity, pre_stock += quantity）
     * 条件 stock >= quantity 并发安全
     */
    int deductStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /**
     * 付款成功：确认预出库（pre_stock -= quantity），库存已在下单时扣减
     * 条件 pre_stock >= quantity 防重复
     */
    int confirmPreStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /**
     * 取消订单：预出库回退至库存（stock += quantity, pre_stock -= quantity）
     */
    int restorePreStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /**
     * 退款：库存回退（stock += quantity）
     */
    int restoreStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
}
