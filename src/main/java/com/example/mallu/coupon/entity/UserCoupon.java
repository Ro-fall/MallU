package com.example.mallu.coupon.entity;

import com.example.mallu.common.mybatis.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCoupon implements BaseEntity {

    private Long id;
    private Long userId;
    private Long couponId;
    private Integer status;
    private String orderNo;
    private LocalDateTime usedTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createBy;
    private Long updateBy;
}
