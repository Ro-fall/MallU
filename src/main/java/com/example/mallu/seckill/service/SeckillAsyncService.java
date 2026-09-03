package com.example.mallu.seckill.service;

import com.example.mallu.seckill.entity.SeckillOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 同步落库基线，仅在 seckill.async=false 的压测对比模式下使用。
 * 默认异步模式由 RabbitMQ 消费者执行落库。
 */
@Service
@RequiredArgsConstructor
public class SeckillAsyncService {
    private final SeckillOrderTransactionService transactionService;

    public SeckillOrder createOrderSynchronously(Long seckillGoodsId, Long userId, Long addressId) {
        return transactionService.createOrder(seckillGoodsId, userId, addressId);
    }
}