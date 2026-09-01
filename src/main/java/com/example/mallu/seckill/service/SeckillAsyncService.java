package com.example.mallu.seckill.service;

import com.example.mallu.common.redis.SeckillRedisService;
import com.example.mallu.seckill.entity.SeckillOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillAsyncService {

    private final ExecutorService virtualThreadExecutor;
    private final SeckillRedisService seckillRedisService;
    private final SeckillOrderTransactionService transactionService;

    /**
     * 异步落库：Redis 库存已扣减，这里扣 MySQL 库存并创建订单。
     * 落库失败时恢复 Redis 库存，保证最终一致性。
     */
    public void createOrderAsync(Long seckillGoodsId, Long userId, Long addressId) {
        virtualThreadExecutor.submit(() -> {
            try {
                transactionService.createOrder(seckillGoodsId, userId, addressId);
            } catch (Exception e) {
                log.error("[秒杀异步] 落库失败, goodsId={}, userId={}", seckillGoodsId, userId, e);
                // 恢复 Redis 库存与用户购买记录，并标记失败
                seckillRedisService.recoverStock(seckillGoodsId, userId);
                seckillRedisService.markResultFailed(seckillGoodsId, userId);
            }
        });
    }

    public SeckillOrder createOrderSynchronously(Long seckillGoodsId, Long userId, Long addressId) {
        return transactionService.createOrder(seckillGoodsId, userId, addressId);
    }
}
