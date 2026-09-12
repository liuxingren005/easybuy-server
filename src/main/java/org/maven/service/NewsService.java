package org.maven.service;

import com.github.pagehelper.PageInfo;
import org.maven.entity.News;

import java.util.List;

/**
 * 资讯服务接口（easybuy_news 表）
 */
public interface NewsService {

    /**
     * 分页条件查询
     */
    PageInfo<News> findPage(int pageNum, int pageSize, String title, String startTime, String endTime);

    /**
     * 查询资讯
     */
    List<News> findAll(int limit);

    /**
     * 根据ID查询
     */
    News findById(Integer id);

    /**
     * 新增资讯
     */
    void add(News news);

    /**
     * 修改资讯
     */
    void modify(News news);

    /**
     * 逻辑删除
     */
    void remove(Integer id);
}
