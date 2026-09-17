package com.rofall.mallu.seckill;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "seckill_order") @Getter @NoArgsConstructor
public class SeckillOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "seckill_goods_id", nullable = false) private Long seckillGoodsId;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String status;
    public SeckillOrder(Long seckillGoodsId, Long orderId, Long userId) { this.seckillGoodsId=seckillGoodsId; this.orderId=orderId; this.userId=userId; this.status="PENDING_PAYMENT"; }
    public void cancel() { status="CANCELLED"; }
}
