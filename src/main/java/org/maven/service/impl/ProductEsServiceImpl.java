package org.maven.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.maven.document.ProductDoc;
import org.maven.entity.Product;
import org.maven.mapper.ProductMapper;
import org.maven.repository.ProductRepository;
import org.maven.service.ProductEsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 商品 Elasticsearch 服务实现类
 * <p>
 * 同步：Repository，高级检索：ElasticsearchOperations
 */
@Service
@RequiredArgsConstructor
public class ProductEsServiceImpl implements ProductEsService {

    private static final Logger log = LoggerFactory.getLogger(ProductEsServiceImpl.class);

    private final ProductMapper productMapper;

    private final ProductRepository productRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 同步
     */
    @Override
    public int sync() {
        List<Product> products = productMapper.selectAll();
        if (CollectionUtils.isEmpty(products)) {
            log.warn("暂无数据，跳过同步");
            return 0;
        }

        List<ProductDoc> docList = new ArrayList<>();
        for (Product product : products) {
            docList.add(toDoc(product));
        }

        productRepository.saveAll(docList);
        log.info("成功同步 {} 条", docList.size());
        return docList.size();
    }

    /**
     * 同步单个商品
     */
    @Override
    public void sync(Product product) {
        try {
            productRepository.save(toDoc(product));
        } catch (Exception e) {
            log.error("同步失败：", e);
        }
    }

    /**
     * 移除商品文档
     */
    @Override
    public void remove(Integer productId) {
        try {
            productRepository.deleteById(productId.toString());
        } catch (Exception e) {
            log.error("删除商品文档失败", e);
        }
    }

    /**
     * NativeQuery 高级检索：bool复杂逻辑 + 高亮 + 分页 + 排序
     */
    @Override
    public PageInfo<ProductDoc> search(String keyword, Integer categoryLevel1Id,
                                       Integer categoryLevel2Id, Integer categoryLevel3Id,
                                       Double minPrice, Double maxPrice, int page, int size) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // must：关键词分词匹配（匹配商品名称和描述）
        if (StringUtils.hasText(keyword)) {
            // 多字段匹配：关键词同时在 name 和 description 中检索，name 权重：3~5
            boolBuilder.must(q -> q
                    .multiMatch(m -> m
                            .fields("name^3", "description")
                            .query(keyword)
                    )
            );
        } else {
            // 无关键词时查询全部
            boolBuilder.must(q -> q.matchAll(b -> b));
        }

        // filter：一级分类ID
        if (categoryLevel1Id != null) {
            boolBuilder.filter(q -> q.term(t ->
                            t.field("categoryLevel1Id")
                                    .value(categoryLevel1Id)
                    )
            );
        }

        // filter：二级分类ID
        if (categoryLevel2Id != null) {
            boolBuilder.filter(q -> q.term(t ->
                            t.field("categoryLevel2Id")
                                    .value(categoryLevel2Id)
                    )
            );
        }

        // filter：三级分类ID
        if (categoryLevel3Id != null) {
            boolBuilder.filter(q -> q.term(t ->
                            t.field("categoryLevel3Id")
                                    .value(categoryLevel3Id)
                    )
            );
        }

        // filter：价格区间
        if (minPrice != null || maxPrice != null) {
            boolBuilder.filter(q -> q
                    .range(r -> r.number(n -> {
                        n.field("price");
                        if (minPrice != null) {
                            n.gte(minPrice);
                        }
                        if (maxPrice != null) {
                            n.lte(maxPrice);
                        }
                        return n;
                    }))
            );
        }

        // 高亮
        Highlight highlight = new Highlight(
                HighlightParameters.builder()
                        .withPreTags("<span style='color:red'>")
                        .withPostTags("</span>")
                        .withFragmentSize(100)
                        .withNumberOfFragments(1)
                        .build(),
                List.of(
                        new HighlightField("name"),
                        new HighlightField("description")
                )
        );

        // 组装 NativeQuery
        int esPage = page - 1;

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolBuilder.build()._toQuery())
                .withPageable(PageRequest.of(esPage, size))             // 分页
                .withSort(Sort.by("createTime").descending())           // 按创建时间倒序
                .withHighlightQuery(new HighlightQuery(highlight, ProductDoc.class))  // 高亮
                .build();

        SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(nativeQuery, ProductDoc.class);

        List<ProductDoc> resultList = getProductDocList(searchHits);

        return createEsPageInfo(searchHits, resultList, esPage, size);
    }

    /**
     * MySQL 商品 → ES 文档
     */
    private ProductDoc toDoc(Product product) {
        ProductDoc doc = new ProductDoc();
        // 类型不同手动拷贝，相同由 BeanUtils 直接拷贝
        BeanUtils.copyProperties(product, doc);
        return doc;
    }

    /**
     * 高亮
     */
    private static List<ProductDoc> getProductDocList(SearchHits<ProductDoc> searchHits) {
        List<ProductDoc> resultList = new ArrayList<>();
        for (SearchHit<ProductDoc> hit : searchHits.getSearchHits()) {
            ProductDoc doc = hit.getContent();

            Map<String, List<String>> highlightMap = hit.getHighlightFields();
            if (highlightMap.containsKey("name")) {
                List<String> nameHighlights = highlightMap.get("name");
                if (!nameHighlights.isEmpty()) {
                    doc.setName(nameHighlights.get(0));
                }
            }
            if (highlightMap.containsKey("description")) {
                List<String> descHighlights = highlightMap.get("description");
                if (!descHighlights.isEmpty()) {
                    doc.setDescription(descHighlights.get(0));
                }
            }
            resultList.add(doc);
        }
        return resultList;
    }

    /**
     * ES 查询分页 PageInfo
     */
    private <T> PageInfo<T> createEsPageInfo(SearchHits<T> searchHits, List<T> processedDataList,
                                             int esPage, int pageSize) {
        long total = searchHits.getTotalHits();

        // ES(0起始) → PageHelper(1起始)
        Page<T> pageObj = new Page<>(esPage + 1, pageSize);
        pageObj.setTotal(total);
        pageObj.addAll(processedDataList);

        return new PageInfo<>(pageObj);
    }
}
