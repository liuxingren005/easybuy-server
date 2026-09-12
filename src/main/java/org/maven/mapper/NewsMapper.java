package org.maven.mapper;

import org.maven.entity.News;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 资讯 Mapper 接口（对应 easybuy_news 表）
 */
public interface NewsMapper {

    /**
     * 分页条件查询
     */
    List<News> selectPage(@Param("title") String title,
                          @Param("startTime") String startTime,
                          @Param("endTime") String endTime);

    /**
     * 查询资讯
     */
    List<News> selectAll(@Param("limit") int limit);

    /** 根据ID查询 */
    News selectById(@Param("id") Integer id);

    /** 根据标题查询 */
    News selectByTitle(@Param("title") String title);

    /** 新增 */
    int insert(News news);

    /** 修改 */
    int update(News news);

    /** 逻辑删除 */
    int deleteById(@Param("id") Integer id);
}
