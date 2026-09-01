package com.example.mallu.seckill.entity;

import com.example.mallu.common.mybatis.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeckillOrder implements BaseEntity {

    private Long id;
    private Long seckillGoodsId;
    private Long orderId;
    private Long userId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createBy;
    private Long updateBy;
}
