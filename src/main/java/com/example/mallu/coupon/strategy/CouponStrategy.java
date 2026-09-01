package com.example.mallu.coupon.strategy;

import java.math.BigDecimal;

public interface CouponStrategy {

    /**
     * 计算优惠金额
     */
    BigDecimal calculateDiscount(BigDecimal totalAmount);
}
