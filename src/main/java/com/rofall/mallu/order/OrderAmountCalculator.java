package com.rofall.mallu.order;

import com.rofall.mallu.coupon.Coupon;
import java.math.*;

public final class OrderAmountCalculator {
    private OrderAmountCalculator() { }
    public static BigDecimal discount(BigDecimal total, Coupon coupon) {
        if (coupon == null) return BigDecimal.ZERO;
        return switch (coupon.getType()) {
            case "FULL_REDUCTION" -> coupon.getThresholdAmount() != null && total.compareTo(coupon.getThresholdAmount()) >= 0
                    ? coupon.getValue().min(total) : BigDecimal.ZERO;
            case "DISCOUNT" -> total.subtract(total.multiply(coupon.getValue())).setScale(2, RoundingMode.HALF_UP);
            case "DIRECT" -> coupon.getValue().min(total);
            default -> BigDecimal.ZERO;
        };
    }
}
