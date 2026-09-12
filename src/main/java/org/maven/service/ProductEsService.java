package org.maven.service;

import com.github.pagehelper.PageInfo;
import org.maven.document.ProductDoc;
import org.maven.entity.Product;

/**
 * 商品 Elasticsearch 服务接口
 * <p>
 * 同步：Repository，高级检索：ElasticsearchOperations
 */
public interface ProductEsService {

    /**
     * 同步
     */
    int sync();

    /**
     * 同步单个商品（新增/修改）
     */
    void sync(Product product);

    /**
     * 移除商品文档（逻辑删除）
     */
    void remove(Integer productId);

    /**
     * NativeQuery 高级检索：bool 复杂逻辑 + 高亮 + 分页 + 排序
     */
    PageInfo<ProductDoc> search(String keyword, Integer categoryLevel1Id, Integer categoryLevel2Id,
                                 Integer categoryLevel3Id, Double minPrice, Double maxPrice, int page, int size);
}
