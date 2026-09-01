package com.example.mallu.user.entity;

import com.example.mallu.common.mybatis.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User implements BaseEntity {

    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createBy;
    private Long updateBy;
}
