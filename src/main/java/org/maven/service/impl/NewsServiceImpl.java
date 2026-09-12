package org.maven.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.maven.entity.News;
import org.maven.exception.BusinessException;
import org.maven.mapper.NewsMapper;
import org.maven.service.NewsService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 资讯服务实现类（easybuy_news 表）
 */
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsMapper newsMapper;

    @Override
    public PageInfo<News> findPage(int pageNum, int pageSize,
                                   String title, String startTime, String endTime) {
        try (Page<?> page = PageHelper.startPage(pageNum, pageSize)) {
            List<News> list = newsMapper.selectPage(title, startTime, endTime);
            return new PageInfo<>(list);
        }
    }

    @Override
    public List<News> findAll(int limit) {
        return newsMapper.selectAll(limit);
    }

    @Override
    public News findById(Integer id) {
        return newsMapper.selectById(id);
    }

    @Override
    public void add(News news) {
        // 标题唯一
        News existing = newsMapper.selectByTitle(news.getTitle());
        if (existing != null) {
            throw new BusinessException("标题已存在");
        }

        // 当前时间 createTime（yyyy-MM-dd）
        news.setCreateTime(new Date());

        if (newsMapper.insert(news) == 0) {
            throw new BusinessException("新增资讯失败");
        }
    }

    @Override
    public void modify(News news) {
        if (news.getTitle() != null) {
            News existing = newsMapper.selectByTitle(news.getTitle());
            if (existing != null && !existing.getId().equals(news.getId())) {
                throw new BusinessException("标题已存在");
            }
        }
        if (newsMapper.update(news) == 0) {
            throw new BusinessException("修改资讯失败");
        }
    }

    @Override
    public void remove(Integer id) {
        if (newsMapper.deleteById(id) == 0) {
            throw new BusinessException("删除资讯失败");
        }
    }
}
