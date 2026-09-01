package com.example.mallu.coupon.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {

    private Long id;
    private String name;
    private Integer type;
    private BigDecimal threshold;
    private BigDecimal discountValue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer remainingCount;
    private Integer status;
}
