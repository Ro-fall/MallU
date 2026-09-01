package com.example.mallu.seckill.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillGoodsVO {

    private Long id;
    private Long activityId;
    private String activityName;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer totalStock;
    private Integer remainingStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
