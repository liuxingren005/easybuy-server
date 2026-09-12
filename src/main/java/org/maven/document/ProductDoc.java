package org.maven.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品 Elasticsearch 文档实体（easybuy_product 索引）
 * <p>
 * ik 分词器 - name/description（索引： ik_max_word，搜索： ik_smart）
 */
@Data
@Document(indexName = "easybuy_product")
public class ProductDoc {

    /**
     * 商品ID
     */
    @Id
    @Field(type = FieldType.Integer)
    private Integer id;

    /**
     * 商品名称（搜索 + 高亮）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /**
     * 商品描述（搜索 + 高亮）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 价格
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 库存
     */
    @Field(type = FieldType.Integer)
    private Integer stock;

    /**
     * 一级分类ID
     */
    @Field(type = FieldType.Integer)
    private Integer categoryLevel1Id;

    /**
     * 二级分类ID
     */
    @Field(type = FieldType.Integer)
    private Integer categoryLevel2Id;

    /**
     * 三级分类ID
     */
    @Field(type = FieldType.Integer)
    private Integer categoryLevel3Id;

    /**
     * 图片文件名
     */
    @Field(type = FieldType.Keyword, index = false)
    private String fileName;

    /**
     * 创建时间（yyyy-MM-dd HH:mm:ss）
     */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
