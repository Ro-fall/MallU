package com.example.mallu.product.controller;

import com.example.mallu.common.interceptor.SkipAuth;
import com.example.mallu.common.result.PageResult;
import com.example.mallu.common.result.Result;
import com.example.mallu.product.dto.ProductStockVO;
import com.example.mallu.product.dto.ProductVO;
import com.example.mallu.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @SkipAuth
    public Result<PageResult<ProductVO>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(productService.listProducts(page, size));
    }

    @GetMapping("/{id}")
    @SkipAuth
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @GetMapping("/{id}/stock")
    @SkipAuth
    public Result<ProductStockVO> stock(@PathVariable Long id) {
        return Result.success(productService.getStock(id));
    }
}
