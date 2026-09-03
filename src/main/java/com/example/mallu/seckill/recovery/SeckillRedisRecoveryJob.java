package com.example.mallu.seckill.recovery;

import com.example.mallu.common.health.RedisAvailabilityService;
import com.example.mallu.common.redis.SeckillRedisService;
import com.example.mallu.seckill.entity.SeckillGoods;
import com.example.mallu.seckill.mapper.SeckillGoodsMapper;
import com.example.mallu.seckill.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillRedisRecoveryJob {

    private final RedisAvailabilityService redisAvailabilityService;
    private final SeckillRedisService seckillRedisService;
    private final SeckillGoodsMapper goodsMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private volatile boolean redisWasAvailable;

    @Scheduled(fixedDelayString = "${redis.recovery-check-ms:30000}", initialDelayString = "${redis.recovery-initial-delay-ms:10000}")
    public void rebuildAfterRedisRecovery() {
        boolean available = redisAvailabilityService.isAvailable();
        if (!available) {
            redisWasAvailable = false;
            return;
        }
        if (redisWasAvailable) {
            return;
        }
        try {
            for (SeckillGoods goods : goodsMapper.selectActiveForRecovery()) {
                seckillRedisService.rebuildStock(goods.getId(), goods.getStock(),
                        seckillOrderMapper.selectActiveUserIdsByGoodsId(goods.getId()));
            }
            redisWasAvailable = true;
            log.info("Redis 恢复完成：秒杀库存与已购用户状态已从 MySQL 重建");
        } catch (RuntimeException e) {
            redisWasAvailable = false;
            log.warn("Redis 恢复重建失败，将在下个周期重试: {}", e.getMessage());
        }
    }
}