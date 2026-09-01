package com.example.mallu.coupon.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    private Long id;
    private Long userId;
    private Long couponId;
    private String couponName;
    private Integer couponType;
    private Integer status;
    private String orderNo;
    private LocalDateTime usedTime;
    private LocalDateTime createdAt;
}
