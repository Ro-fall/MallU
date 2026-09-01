package com.example.mallu.coupon.strategy;

import java.math.BigDecimal;

public class FullReductionStrategy implements CouponStrategy {

    private final BigDecimal threshold;
    private final BigDecimal discountValue;

    public FullReductionStrategy(BigDecimal threshold, BigDecimal discountValue) {
        this.threshold = threshold;
        this.discountValue = discountValue;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal totalAmount) {
        if (threshold != null && totalAmount.compareTo(threshold) >= 0) {
            return discountValue.min(totalAmount);
        }
        return BigDecimal.ZERO;
    }
}
