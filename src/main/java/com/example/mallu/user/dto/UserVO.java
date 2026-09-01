package com.example.mallu.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createdAt;
}
