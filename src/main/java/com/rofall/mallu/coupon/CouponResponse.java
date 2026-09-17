package com.rofall.mallu.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(Long userCouponId, Long couponId, String name, String type, BigDecimal thresholdAmount,
                             BigDecimal value, String status, LocalDateTime endTime) {
}
