package com.rofall.mallu.catalog;

import com.rofall.mallu.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<CategoryResponse> categories() {
        return categoryRepository.findByStatusTrueOrderBySortOrderAsc().stream().map(CategoryResponse::from).toList();
    }

    public Page<CatalogResponse> products(Long categoryId, int page, int size) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50));
        Page<Product> result = categoryId == null
                ? productRepository.findByStatusTrue(request)
                : productRepository.findByStatusTrueAndCategoryId(categoryId, request);
        return result.map(CatalogResponse::from);
    }

    public List<CatalogResponse> recommendations() {
        return productRepository.findTop10ByStatusTrueAndRecommendedTrueOrderByIdDesc().stream()
                .map(CatalogResponse::from).toList();
    }

    public Page<CatalogResponse> search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(4002, HttpStatus.BAD_REQUEST, "搜索关键词不能为空");
        }
        return productRepository
                .findByStatusTrueAndNameContainingIgnoreCaseOrStatusTrueAndDescriptionContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50)))
                .map(CatalogResponse::from);
    }

    public CatalogResponse detail(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(Product::getStatus)
                .orElseThrow(() -> new BusinessException(4042, HttpStatus.NOT_FOUND, "商品不存在或已下架"));
        return CatalogResponse.from(product);
    }
}
