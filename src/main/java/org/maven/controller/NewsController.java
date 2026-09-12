package org.maven.controller;

import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.maven.annotation.RequireRole;
import org.maven.common.ResponseResult;
import org.maven.common.Role;
import org.maven.entity.News;
import org.maven.service.NewsService;
import org.springframework.web.bind.annotation.*;

/**
 * 资讯 Controller（easybuy_news 表）
 * <p>
 * 管理员：分页查询、新增、修改、删除（逻辑删除）
 * 普通用户：查看资讯列表
 */
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    /**
     * 分页条件查询
     * <p>
     * GET /news/page?pageNum=1&pageSize=5&title=&startTime=&endTime=
     * startTime/endTime yyyy-MM-dd
     */
    @RequireRole(Role.ADMIN)
    @GetMapping("/page")
    public ResponseResult findPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        PageInfo<News> pageInfo = newsService.findPage(pageNum, pageSize, title, startTime, endTime);
        return ResponseResult.success().put("page", pageInfo);
    }

    /**
     * 查询资讯列表
     * <p>
     * GET /news/list?limit=10
     */
    @GetMapping("/list")
    public ResponseResult findAll(@RequestParam(defaultValue = "10") int limit) {
        return ResponseResult.success().put("list", newsService.findAll(limit));
    }

    /**
     * 根据ID查询资讯详情
     * <p>
     * GET /news/{id}
     */
    @GetMapping("/{id}")
    public ResponseResult findById(@PathVariable Integer id) {
        News news = newsService.findById(id);
        if (news == null) {
            return ResponseResult.error("资讯不存在");
        }
        return ResponseResult.success().put("data", news);
    }

    /**
     * 新增资讯
     * <p>
     * POST /news
     * Body: { "title": "", "content": "", "createTime": "" }
     */
    @RequireRole(Role.ADMIN)
    @PostMapping
    public ResponseResult add(@RequestBody News news) {
        newsService.add(news);
        return ResponseResult.success().put("data", news);
    }

    /**
     * 修改资讯
     * <p>
     * PUT /news
     * Body: { "id": 1, "title": "", "content": "", "createTime": "" }
     */
    @RequireRole(Role.ADMIN)
    @PutMapping
    public ResponseResult update(@RequestBody News news) {
        if (news.getId() == null) {
            return ResponseResult.error("ID不能为空");
        }
        newsService.modify(news);
        return ResponseResult.success();
    }

    /**
     * 逻辑删除资讯
     * <p>
     * DELETE /news/{id}
     */
    @RequireRole(Role.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseResult delete(@PathVariable Integer id) {
        newsService.remove(id);
        return ResponseResult.success();
    }
}
