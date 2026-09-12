package org.maven.controller;

import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.maven.annotation.RequireRole;
import org.maven.common.ResponseResult;
import org.maven.common.Role;
import org.maven.entity.ProductCategory;
import org.maven.service.ProductCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类控制器
 */
@RestController
@RequestMapping("/productCategory")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    /**
     * 查询分类树形结构
     */
    @GetMapping("/tree")
    public ResponseResult tree() {
        List<ProductCategory> list = productCategoryService.findCategoryTree();
        return ResponseResult.success().put("list", list);
    }

    /**
     * 查询所有分类
     */
    @GetMapping("/all")
    public ResponseResult all() {
        List<ProductCategory> list = productCategoryService.findAll();
        return ResponseResult.success().put("list", list);
    }

    /**
     * 分页条件查询
     */
    @RequireRole(Role.ADMIN)
    @GetMapping("/page")
    public ResponseResult page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type) {
        List<ProductCategory> list = productCategoryService.findPage(name, type,
                pageNum, pageSize);
        return ResponseResult.success().put("page", new PageInfo<>(list));
    }

    /**
     * 根据父级ID查询子分类
     */
    @GetMapping("/parent/{parentId}")
    public ResponseResult findByParentId(@PathVariable Integer parentId) {
        List<ProductCategory> list = productCategoryService.findByParentId(parentId);
        return ResponseResult.success().put("list", list);
    }

    /**
     * 根据类型查询分类
     */
    @GetMapping("/type/{type}")
    public ResponseResult findByType(@PathVariable Integer type) {
        List<ProductCategory> list = productCategoryService.findByType(type);
        return ResponseResult.success().put("list", list);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public ResponseResult detail(@PathVariable Integer id) {
        ProductCategory productCategory = productCategoryService.findById(id);
        return ResponseResult.success().put("data", productCategory);
    }

    /**
     * 新增
     */
    @RequireRole(Role.ADMIN)
    @PostMapping
    public ResponseResult add(@RequestBody ProductCategory productCategory) {
        productCategoryService.add(productCategory);
        return ResponseResult.success();
    }

    /**
     * 修改
     */
    @RequireRole(Role.ADMIN)
    @PutMapping
    public ResponseResult update(@RequestBody ProductCategory productCategory) {
        productCategoryService.modify(productCategory);
        return ResponseResult.success();
    }

    /**
     * 删除
     */
    @RequireRole(Role.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseResult delete(@PathVariable Integer id) {
        productCategoryService.removeById(id);
        return ResponseResult.success();
    }
}
