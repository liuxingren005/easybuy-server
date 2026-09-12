package org.maven.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.maven.entity.ProductCategory;
import org.maven.exception.BusinessException;
import org.maven.mapper.ProductCategoryMapper;
import org.maven.service.ProductCategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类业务实现类
 */
@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryMapper productCategoryMapper;

    public ProductCategoryServiceImpl(ProductCategoryMapper productCategoryMapper) {
        this.productCategoryMapper = productCategoryMapper;
    }

    @Override
    public List<ProductCategory> findAll() {
        return productCategoryMapper.selectAll();
    }

    /**
     * 查询一级分类及其子分类（树形结构）
     * <p>
     * 递归挂载
     */
    @Override
    public List<ProductCategory> findCategoryTree() {
        List<ProductCategory> all = productCategoryMapper.selectAll();
        // 分组
        Map<Integer, List<ProductCategory>> parentMap = all.stream()
                .collect(Collectors.groupingBy(
                                c -> c.getParentId() == null ? 0 : c.getParentId()
                        )
                );
        // 挂载
        List<ProductCategory> roots = parentMap.getOrDefault(0, new ArrayList<>()); // 顶级父节点，默认空列表
        for (ProductCategory root : roots) {
            setChildren(root, parentMap);
        }
        return roots;
    }

    /**
     * 递归挂载子分类
     */
    private void setChildren(ProductCategory parent, Map<Integer, List<ProductCategory>> parentMap) {
        List<ProductCategory> children = parentMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildCategory(children);
            for (ProductCategory child : children) {
                setChildren(child, parentMap);
            }
        }
    }

    @Override
    public List<ProductCategory> findByParentId(Integer parentId) {
        return productCategoryMapper.selectByParentId(parentId);
    }

    @Override
    public List<ProductCategory> findByType(Integer type) {
        return productCategoryMapper.selectByType(type);
    }

    @Override
    public ProductCategory findById(Integer id) {
        return productCategoryMapper.selectById(id);
    }

    /**
     * 新增
     */
    @Override
    public void add(ProductCategory productCategory) {
        // 同级分类名称唯一
        ProductCategory exist = productCategoryMapper.selectByNameAndParentId(
                productCategory.getName(), productCategory.getParentId());
        if (exist != null) {
            throw new BusinessException("同级分类下已存在该名称");
        }
        if (productCategoryMapper.insert(productCategory) == 0) {
            throw new BusinessException("新增分类失败");
        }
    }

    /**
     * 修改
     */
    @Override
    public void modify(ProductCategory productCategory) {
        // 同级分类名称唯一（排除自身）
        if (productCategory.getName() != null && !productCategory.getName().isEmpty()) {
            ProductCategory exist = productCategoryMapper.selectByNameAndParentId(
                    productCategory.getName(), productCategory.getParentId());
            if (exist != null && !exist.getId().equals(productCategory.getId())) {
                throw new BusinessException("同级分类下已存在该名称");
            }
        }
        if (productCategoryMapper.update(productCategory) == 0) {
            throw new BusinessException("修改分类失败");
        }
    }

    /**
     * 删除（逻辑删除）
     */
    @Override
    public void removeById(Integer id) {
        // 是否有子分类
        int childCount = productCategoryMapper.countByParentId(id);
        if (childCount > 0) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }
        // 是否被商品引用
        int productCount = productCategoryMapper.countProductByCategoryId(id);
        if (productCount > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }
        if (productCategoryMapper.deleteById(id) == 0) {
            throw new BusinessException("删除分类失败");
        }
    }

    @Override
    public List<ProductCategory> findPage(String name, Integer type,
                                          Integer pageNum, Integer pageSize) {
        try (Page<?> page = PageHelper.startPage(pageNum, pageSize)) {
            return productCategoryMapper.selectPage(name, type);
        }
    }
}
