package com.example.mallu.coupon.strategy;

import java.math.BigDecimal;

public class DiscountStrategy implements CouponStrategy {

    private final BigDecimal discountValue;

    public DiscountStrategy(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal totalAmount) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0
                || discountValue.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal payAmount = totalAmount.multiply(discountValue);
        return totalAmount.subtract(payAmount).max(BigDecimal.ZERO);
    }
}
