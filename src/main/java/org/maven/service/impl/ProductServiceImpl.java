package org.maven.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.maven.entity.Product;
import org.maven.exception.BusinessException;
import org.maven.mapper.ProductMapper;
import org.maven.service.ProductEsService;
import org.maven.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品业务实现类
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    private final ProductEsService productEsService;

    @Override
    public List<Product> findPage(String name, String startTime, String endTime,
                                  Integer categoryLevel1Id, Integer categoryLevel2Id,
                                  Integer categoryLevel3Id, Integer pageNum, Integer pageSize) {
        try (Page<?> page = PageHelper.startPage(pageNum, pageSize)) {
            return productMapper.selectPage(name, startTime, endTime,
                    categoryLevel1Id, categoryLevel2Id, categoryLevel3Id);
        }
    }

    @Override
    public List<Product> findAll() {
        return productMapper.selectAll();
    }

    @Override
    public List<Product> findByCategoryLevel1Id(Integer categoryLevel1Id, Integer limit) {
        return productMapper.selectByCategoryLevel1Id(categoryLevel1Id, limit);
    }

    @Override
    public List<Product> findByCategoryLevel2Id(Integer categoryLevel2Id, Integer limit) {
        return productMapper.selectByCategoryLevel2Id(categoryLevel2Id, limit);
    }

    @Override
    public List<Product> findByCategoryLevel3Id(Integer categoryLevel3Id, Integer limit) {
        return productMapper.selectByCategoryLevel3Id(categoryLevel3Id, limit);
    }

    @Override
    public Product findById(Integer id) {
        return productMapper.selectById(id);
    }

    @Override
    public List<Product> findHot(Integer limit) {
        return productMapper.selectHot(limit);
    }

    @Override
    public void add(Product product) {
        if (productMapper.insert(product) == 0) {
            throw new BusinessException("新增商品失败");
        }

        // 同步更新（回填时间）
        Product existProduct = productMapper.selectById(product.getId());
        productEsService.sync(existProduct != null
                ? existProduct : product);
    }

    @Override
    public void modify(Product product) {
        if (productMapper.update(product) == 0) {
            throw new BusinessException("修改商品失败");
        }

        // 同步更新，查不到不同步
        Product latestProduct = productMapper.selectById(product.getId());
        if (latestProduct != null) {
            productEsService.sync(latestProduct);
        }
    }

    @Override
    public void removeById(Integer id) {
        if (productMapper.deleteById(id) == 0) {
            throw new BusinessException("删除商品失败");
        }

        // 同步更新
        productEsService.remove(id);
    }
}
