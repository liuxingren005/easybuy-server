package org.maven.controller;

import lombok.RequiredArgsConstructor;
import org.maven.common.ResponseResult;
import org.maven.security.UserContext;
import org.maven.service.FavoriteService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 收藏夹 Controller（Redis List 存储）
 */
@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 查询当前用户收藏列表
     *
     * GET /favorite
     */
    @GetMapping
    public ResponseResult list() {
        return ResponseResult.success().put("list", favoriteService.list(UserContext.getUserId()));
    }

    /**
     * 添加收藏
     *
     * POST /favorite  Body: { "productId": 1 }
     */
    @PostMapping
    public ResponseResult add(@RequestBody Map<String, Object> body) {
        Integer productId = toInt(body.get("productId"));
        return ResponseResult.success()
                .put("list", favoriteService.add(UserContext.getUserId(), productId));
    }

    /**
     * 移除单项收藏
     *
     * DELETE /favorite/{productId}
     */
    @DeleteMapping("/{productId}")
    public ResponseResult remove(@PathVariable Integer productId) {
        return ResponseResult.success()
                .put("list", favoriteService.remove(UserContext.getUserId(), productId));
    }

    /**
     * 清空收藏
     *
     * DELETE /favorite
     */
    @DeleteMapping
    public ResponseResult clear() {
        return ResponseResult.success().put("list", favoriteService.clear(UserContext.getUserId()));
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
