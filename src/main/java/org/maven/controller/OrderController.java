package org.maven.controller;

import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.maven.annotation.RequireRole;
import org.maven.common.ResponseResult;
import org.maven.common.Role;
import org.maven.entity.Order;
import org.maven.request.Refund;
import org.maven.security.UserContext;
import org.maven.service.OrderService;
import org.maven.service.AfterSaleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AfterSaleService afterSaleService;

    /**
     * 管理员分页查询全部订单
     * 查询条件：用户名、订单号、付款状态、创建时间
     */
    @RequireRole(Role.ADMIN)
    @GetMapping("/page")
    public ResponseResult page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String loginName,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        List<Order> list = orderService.findPage(loginName, serialNumber, status,
                startTime, endTime, pageNum, pageSize);
        return ResponseResult.success().put("page", new PageInfo<>(list));
    }

    /**
     * 用户分页查询自己的订单
     * 查询条件：订单号、付款状态、创建时间
     */
    @GetMapping("/my/page")
    public ResponseResult myPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Integer userId = UserContext.getUserId();
        List<Order> list = orderService.findPageByUserId(userId, serialNumber, status,
                startTime, endTime, pageNum, pageSize);
        return ResponseResult.success().put("page", new PageInfo<>(list));
    }

    /**
     * 根据ID查询详情（订单明细）
     */
    @GetMapping("/{id}")
    public ResponseResult detail(@PathVariable Integer id) {
        Order order = orderService.findById(id);
        return ResponseResult.success().put("data", order);
    }

    /**
     * 根据用户ID查询订单列表
     */
    @GetMapping("/user/{userId}")
    public ResponseResult findByUserId(@PathVariable Integer userId) {
        List<Order> list = orderService.findByUserId(userId);
        return ResponseResult.success().put("list", list);
    }

    /**
     * 新增订单
     */
    @PostMapping
    public ResponseResult add(@RequestBody Order order) {
        orderService.add(order);
        return ResponseResult.success().put("orderId", order.getId());
    }

    /**
     * 修改订单
     */
    @PutMapping
    public ResponseResult update(@RequestBody Order order) {
        orderService.modify(order);
        return ResponseResult.success();
    }

    /**
     * 关闭订单（仅待付款状态关闭）
     */
    @PutMapping("/{id}/close")
    public ResponseResult close(@PathVariable Integer id) {
        orderService.closeOrder(id);
        return ResponseResult.success();
    }

    /**
     * 支付订单（仅待付款状态支付）
     */
    @PutMapping("/{id}/pay")
    public ResponseResult pay(@PathVariable Integer id) {
        orderService.payOrder(id);
        return ResponseResult.success();
    }

    /**
     * 支付订单（指定支付方式）
     * @param id 订单ID
     * @param payType 支付方式（1:支付宝 2:微信）
     * @param transactionId 第三方交易号（可选）
     */
    @PutMapping("/{id}/pay/{payType}")
    public ResponseResult payWithType(@PathVariable Integer id,
                                      @PathVariable Integer payType,
                                      @RequestParam(required = false) String transactionId) {
        orderService.payOrder(id, payType, transactionId);
        return ResponseResult.success();
    }

    /**
     * 退款（整单全额退款与按明细部分退款）JSON/XML
     */
    @PutMapping("/{id}/refund")
    public ResponseResult refund(@PathVariable Integer id,
                                 @RequestBody(required = false) Refund body) {
        if (body == null) {
            afterSaleService.refundOrder(id); // 全额退款
        } else {
            afterSaleService.refundOrder(id, body.getItems(), body.getReason()); // 明细退款
        }
        return ResponseResult.success();
    }

    /**
     * 查询订单退款（部分退款明细）
     */
    @GetMapping("/{id}/refunds")
    public ResponseResult refunds(@PathVariable Integer id) {
        return ResponseResult.success().put("list", orderService.findRefundsByOrderId(id));
    }

    /**
     * 删除订单（逻辑删除，仅已关闭状态删除）
     */
    @DeleteMapping("/{id}")
    public ResponseResult delete(@PathVariable Integer id) {
        orderService.removeById(id);
        return ResponseResult.success();
    }
}
