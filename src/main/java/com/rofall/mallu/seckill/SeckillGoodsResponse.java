package com.rofall.mallu.seckill;
import java.math.BigDecimal;
public record SeckillGoodsResponse(Long id, Long activityId, Long productId, String productName, BigDecimal price, int stock, boolean active) { }
