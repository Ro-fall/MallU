package com.example.mallu.user.entity;

import com.example.mallu.common.mybatis.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Address implements BaseEntity {

    private Long id;
    private Long userId;
    private String receiverName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createBy;
    private Long updateBy;
}
