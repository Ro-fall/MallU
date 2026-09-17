package com.rofall.mallu.seckill;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity @Table(name = "seckill_goods") @Getter @NoArgsConstructor
public class SeckillGoods {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "activity_id", nullable = false) private Long activityId;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "seckill_price", nullable = false) private BigDecimal seckillPrice;
    @Column(nullable = false) private Integer stock;
    @Column(name = "total_stock", nullable = false) private Integer totalStock;
    @Column(name = "per_user_limit", nullable = false) private Integer perUserLimit;
    public void decreaseStock() { if (stock <= 0) throw new IllegalStateException("秒杀库存不足"); stock--; }
    public void increaseStock() { if (stock < totalStock) stock++; }
}
