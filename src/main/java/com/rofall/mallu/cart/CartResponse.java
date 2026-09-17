package com.rofall.mallu.cart;

import java.math.BigDecimal;

public record CartResponse(Long id, Long productId, String productName, BigDecimal price,
                           int quantity, int availableStock) {
}
