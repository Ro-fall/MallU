package com.rofall.mallu.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByStatusTrue(Pageable pageable);
    Page<Product> findByStatusTrueAndCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findByStatusTrueAndNameContainingIgnoreCaseOrStatusTrueAndDescriptionContainingIgnoreCase(
            String nameKeyword, String descriptionKeyword, Pageable pageable);
    List<Product> findTop10ByStatusTrueAndRecommendedTrueOrderByIdDesc();
}
