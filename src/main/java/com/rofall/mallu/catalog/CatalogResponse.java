package com.rofall.mallu.catalog;

import java.math.BigDecimal;

public record CatalogResponse(Long id, Long categoryId, String name, String description,
                              BigDecimal price, int stock, boolean recommended) {
    static CatalogResponse from(Product product) {
        return new CatalogResponse(product.getId(), product.getCategoryId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), product.getRecommended());
    }
}
