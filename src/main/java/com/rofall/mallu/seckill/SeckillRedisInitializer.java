package com.rofall.mallu.seckill;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SeckillRedisInitializer {
    private final SeckillGoodsRepository goodsRepository;
    private final SeckillRedisService redisService;
    public SeckillRedisInitializer(SeckillGoodsRepository goodsRepository, SeckillRedisService redisService) { this.goodsRepository=goodsRepository; this.redisService=redisService; }
    @EventListener(ApplicationReadyEvent.class)
    public void rebuildMissingStock() { goodsRepository.findAll().forEach(redisService::initializeIfAbsent); }

    @Scheduled(fixedDelayString = "${mallu.seckill.redis-rebuild-ms:30000}")
    public void rebuildMissingStockPeriodically() { rebuildMissingStock(); }
}
