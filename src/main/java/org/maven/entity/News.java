package org.maven.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 资讯实体类（easybuy_news 表）
 */
@Data
public class News {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 标题（唯一约束）
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建时间（yyyy-MM-dd）
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date createTime;

    /**
     * 是否删除（1:已删除 0:未删除）
     */
    private Integer isDelete;
}
