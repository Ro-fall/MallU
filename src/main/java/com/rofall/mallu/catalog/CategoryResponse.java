package com.rofall.mallu.catalog;

public record CategoryResponse(Long id, String name, int sortOrder) {
    static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSortOrder());
    }
}
