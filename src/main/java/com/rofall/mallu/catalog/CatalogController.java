package com.rofall.mallu.catalog;

import com.rofall.mallu.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalogService;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.success(catalogService.categories());
    }

    @GetMapping("/products")
    public ApiResponse<Page<CatalogResponse>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.success(catalogService.products(categoryId, page, size));
    }

    @GetMapping("/products/recommendations")
    public ApiResponse<List<CatalogResponse>> recommendations() {
        return ApiResponse.success(catalogService.recommendations());
    }

    @GetMapping("/products/search")
    public ApiResponse<Page<CatalogResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.success(catalogService.search(keyword, page, size));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<CatalogResponse> detail(@PathVariable Long productId) {
        return ApiResponse.success(catalogService.detail(productId));
    }
}
