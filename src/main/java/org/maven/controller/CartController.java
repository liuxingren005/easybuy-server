package org.maven.controller;

import lombok.RequiredArgsConstructor;
import org.maven.common.ResponseResult;
import org.maven.security.UserContext;
import org.maven.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 购物车 Controller（Redis 存储）
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 查询当前用户购物车列表
     *
     * GET /cart
     */
    @GetMapping
    public ResponseResult list() {
        return ResponseResult.success().put("list", cartService.list(UserContext.getUserId()));
    }

    /**
     * 加入购物车
     *
     * POST /cart  Body: { "productId": 1, "quantity": 1 }
     */
    @PostMapping
    public ResponseResult add(@RequestBody Map<String, Object> body) {
        Integer productId = toInt(body.get("productId"));
        Integer quantity = toInt(body.get("quantity"));
        if (quantity == null) {
            quantity = 1; // 缺省加 1 件
        }
        return ResponseResult.success()
                .put("list", cartService.add(UserContext.getUserId(), productId, quantity));
    }

    /**
     * 修改购买数量
     *
     * PUT /cart/{productId}  Body: { "quantity": 2 }
     */
    @PutMapping("/{productId}")
    public ResponseResult updateQuantity(@PathVariable Integer productId,
                                         @RequestBody Map<String, Object> body) {
        return ResponseResult.success()
                .put("list", cartService.updateQuantity(UserContext.getUserId(), productId,
                        toInt(body.get("quantity"))));
    }

    /**
     * 移除单项
     *
     * DELETE /cart/{productId}
     */
    @DeleteMapping("/{productId}")
    public ResponseResult remove(@PathVariable Integer productId) {
        return ResponseResult.success()
                .put("list", cartService.remove(UserContext.getUserId(), productId));
    }

    /**
     * 清空购物车
     *
     * DELETE /cart
     */
    @DeleteMapping
    public ResponseResult clear() {
        return ResponseResult.success().put("list", cartService.clear(UserContext.getUserId()));
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
