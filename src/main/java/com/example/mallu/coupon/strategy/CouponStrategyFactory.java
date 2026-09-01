package com.example.mallu.coupon.strategy;

import com.example.mallu.coupon.entity.Coupon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CouponStrategyFactory {

    public CouponStrategy getStrategy(Coupon coupon) {
        if (coupon == null) {
            return totalAmount -> BigDecimal.ZERO;
        }
        return switch (coupon.getType()) {
            case 1 -> new FullReductionStrategy(coupon.getThreshold(), coupon.getDiscountValue());
            case 2 -> new DiscountStrategy(coupon.getDiscountValue());
            case 3 -> new DirectReductionStrategy(coupon.getDiscountValue());
            default -> throw new IllegalArgumentException("不支持的优惠券类型: " + coupon.getType());
        };
    }
}
