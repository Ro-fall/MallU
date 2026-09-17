package com.rofall.mallu.cart;

import jakarta.validation.constraints.Min;

public record CartUpdateRequest(@Min(1) int quantity) {
}
