package org.maven.controller;

import org.maven.common.ResponseResult;
import org.maven.entity.UserAddress;
import org.maven.service.UserAddressService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户地址控制器
 */
@RestController
@RequestMapping("/userAddress")
public class UserAddressController {

    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    /**
     * 根据用户ID查询地址列表
     */
    @GetMapping("/user/{userId}")
    public ResponseResult findByUserId(@PathVariable Integer userId) {
        List<UserAddress> list = userAddressService.findByUserId(userId);
        return ResponseResult.success().put("list", list);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public ResponseResult detail(@PathVariable Integer id) {
        UserAddress userAddress = userAddressService.findById(id);
        return ResponseResult.success().put("data", userAddress);
    }

    /**
     * 查询用户默认地址
     */
    @GetMapping("/default/{userId}")
    public ResponseResult findDefault(@PathVariable Integer userId) {
        UserAddress userAddress = userAddressService.findDefaultByUserId(userId);
        return ResponseResult.success().put("data", userAddress);
    }

    /**
     * 新增
     */
    @PostMapping
    public ResponseResult add(@RequestBody UserAddress userAddress) {
        userAddressService.add(userAddress);
        return ResponseResult.success();
    }

    /**
     * 修改
     */
    @PutMapping
    public ResponseResult update(@RequestBody UserAddress userAddress) {
        userAddressService.modify(userAddress);
        return ResponseResult.success();
    }

    /**
     * 逻辑删除
     */
    @DeleteMapping("/{id}")
    public ResponseResult delete(@PathVariable Integer id) {
        userAddressService.removeById(id);
        return ResponseResult.success();
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/default/{id}/{userId}")
    public ResponseResult setDefault(@PathVariable Integer id,
                                     @PathVariable Integer userId) {
        userAddressService.setDefault(id, userId);
        return ResponseResult.success();
    }
}
