package com.example.mallu.coupon.strategy;

import java.math.BigDecimal;

public class DirectReductionStrategy implements CouponStrategy {

    private final BigDecimal discountValue;

    public DirectReductionStrategy(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal totalAmount) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return discountValue.min(totalAmount);
    }
}
