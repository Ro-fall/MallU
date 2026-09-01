package com.example.mallu.seckill.entity;

import com.example.mallu.common.mybatis.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillGoods implements BaseEntity {

    private Long id;
    private Long activityId;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer totalStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createBy;
    private Long updateBy;
}
