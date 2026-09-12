package org.maven.controller;

import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.maven.annotation.RequireRole;
import org.maven.common.ResponseResult;
import org.maven.common.Role;
import org.maven.config.SftpProperties;
import org.maven.document.ProductDoc;
import org.maven.entity.Product;
import org.maven.exception.BusinessException;
import org.maven.service.ProductEsService;
import org.maven.service.ProductService;
import org.maven.service.SftpService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Objects;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductEsService productEsService;
    private final SftpService sftpService;
    private final SftpProperties sftpProperties;

    /**
     * 分页条件查询
     */
    @RequireRole(Role.ADMIN)
    @GetMapping("/page")
    public ResponseResult page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer categoryLevel1Id,
            @RequestParam(required = false) Integer categoryLevel2Id,
            @RequestParam(required = false) Integer categoryLevel3Id) {
        List<Product> list = productService.findPage(name, startTime, endTime,
                categoryLevel1Id, categoryLevel2Id, categoryLevel3Id, pageNum, pageSize);
        return ResponseResult.success().put("page", new PageInfo<>(list));
    }

    /**
     * 前台-关键字分页搜索（Elasticsearch：bool + 高亮 + 排序）
     * <p>
     * GET /product/search?keyword=&categoryLevel1Id=&categoryLevel2Id=&categoryLevel3Id=&minPrice=&maxPrice=&pageNum=1&pageSize=12
     */
    @GetMapping("/search")
    public ResponseResult search(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Integer categoryLevel1Id,
                                 @RequestParam(required = false) Integer categoryLevel2Id,
                                 @RequestParam(required = false) Integer categoryLevel3Id,
                                 @RequestParam(required = false) Double minPrice,
                                 @RequestParam(required = false) Double maxPrice,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "12") Integer pageSize) {
        PageInfo<ProductDoc> page = productEsService.search(keyword, categoryLevel1Id,
                categoryLevel2Id, categoryLevel3Id, minPrice, maxPrice, pageNum, pageSize);
        return ResponseResult.success().put("page", page);
    }

    /**
     * 同步
     * <p>
     * POST /product/es/rebuild
     */
    @RequireRole(Role.ADMIN)
    @PostMapping("/es/rebuild")
    public ResponseResult rebuildIndex() {
        int count = productEsService.sync();
        return ResponseResult.success().put("count", count);
    }

    /**
     * 管理员-商品图片上传（SFTP 文件服务器）
     * <p>
     * POST /product/upload  multipart: file
     * { fileName: "xxx.jpg", url: "远程根目录/文件名" }
     */
    @RequireRole(Role.ADMIN)
    @PostMapping("/upload")
    public ResponseResult upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseResult.error("请选择要上传的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseResult.error("仅支持上传图片文件");
        }
        try {
            sftpService.login();
            String remotePath = sftpService.uploadFile(file, sftpProperties.getRemoteRoot());
            String fileName = remotePath.substring(remotePath.lastIndexOf('/') + 1);
            return Objects.requireNonNull(ResponseResult.success()
                            .put("fileName", fileName)) // 防御
                    .put("url", remotePath);
        } catch (Exception e) {
            throw new BusinessException("图片上传失败：" + e.getMessage());
        } finally {
            sftpService.logout();
        }
    }

    /**
     * 管理员-商品图片删除（SFTP 文件服务器）
     * <p>
     * DELETE /product/file?fileName=xxx.jpg
     */
    @RequireRole(Role.ADMIN)
    @DeleteMapping("/file")
    public ResponseResult deleteFile(@RequestParam("fileName") String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("/") || fileName.contains("..")) {
            return ResponseResult.error("非法文件名");
        }
        try {
            sftpService.login();
            sftpService.delete(sftpProperties.getRemoteRoot(), fileName);
            return ResponseResult.success();
        } catch (Exception e) {
            throw new BusinessException("图片删除失败：" + e.getMessage());
        } finally {
            sftpService.logout();
        }
    }

    /**
     * 商品图片访问（读取回传）
     * <p>
     * GET /product/image/{fileName}
     */
    @GetMapping("/image/{fileName}")
    public void image(@PathVariable String fileName, HttpServletResponse response) {
        try {
            sftpService.login();
            byte[] data = sftpService.download(sftpProperties.getRemoteRoot(), fileName);
            String contentType = URLConnection.guessContentTypeFromName(fileName);
            response.setContentType(contentType != null ? contentType : "image/jpeg"); // MIME Type
            response.setHeader("Cache-Control", "max-age=86400"); // 24 小时
            try (OutputStream out = response.getOutputStream()) {
                out.write(data);
                out.flush();
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } finally {
            sftpService.logout();
        }
    }

    /**
     * 前台-根据一级分类查询商品
     */
    @GetMapping("/category1/{id}")
    public ResponseResult findByCategoryLevel1Id(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Product> list = productService.findByCategoryLevel1Id(id, limit);
        return ResponseResult.success().put("list", list);
    }

    /**
     * 前台-根据二级分类查询商品
     */
    @GetMapping("/category2/{id}")
    public ResponseResult findByCategoryLevel2Id(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Product> list = productService.findByCategoryLevel2Id(id, limit);
        return ResponseResult.success().put("list", list);
    }

    /**
     * 前台-根据三级分类查询商品
     */
    @GetMapping("/category3/{id}")
    public ResponseResult findByCategoryLevel3Id(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Product> list = productService.findByCategoryLevel3Id(id, limit);
        return ResponseResult.success().put("list", list);
    }

    /**
     * 根据ID查询详情
     */
    @GetMapping("/{id}")
    public ResponseResult detail(@PathVariable Integer id) {
        Product product = productService.findById(id);
        return ResponseResult.success().put("data", product);
    }

    /**
     * 前台-热门推荐（随机抽取已有商品）
     * <p>
     * GET /product/hot?limit=8
     */
    @GetMapping("/hot")
    public ResponseResult hot(@RequestParam(defaultValue = "8") Integer limit) {
        List<Product> list = productService.findHot(limit);
        return ResponseResult.success().put("list", list);
    }

    /**
     * 新增
     */
    @RequireRole(Role.ADMIN)
    @PostMapping
    public ResponseResult add(@RequestBody Product product) {
        productService.add(product);
        return ResponseResult.success();
    }

    /**
     * 修改
     */
    @RequireRole(Role.ADMIN)
    @PutMapping
    public ResponseResult update(@RequestBody Product product) {
        productService.modify(product);
        return ResponseResult.success();
    }

    /**
     * 逻辑删除
     */
    @RequireRole(Role.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseResult delete(@PathVariable Integer id) {
        productService.removeById(id);
        return ResponseResult.success();
    }
}
