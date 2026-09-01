package com.example.mallu.order.entity;

import com.example.mallu.common.mybatis.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderEventLog implements BaseEntity {

    private Long id;
    private Long orderId;
    private String eventType;
    private String content;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createBy;
    private Long updateBy;
}
