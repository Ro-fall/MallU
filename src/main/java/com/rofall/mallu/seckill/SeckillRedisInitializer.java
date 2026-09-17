package com.rofall.mallu.seckill;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillRedisInitializer {
    private final SeckillGoodsRepository goodsRepository;
    private final SeckillRedisService redisService;
    public SeckillRedisInitializer(SeckillGoodsRepository goodsRepository, SeckillRedisService redisService) { this.goodsRepository=goodsRepository; this.redisService=redisService; }
    @EventListener(ApplicationReadyEvent.class)
    public void rebuildStock() { goodsRepository.findAll().forEach(redisService::initialize); }
}
