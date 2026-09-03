package com.example.mallu.seckill.mq;

public record SeckillOrderMessage(Long seckillGoodsId, Long userId, Long addressId, int retryCount) {
    public SeckillOrderMessage nextRetry() {
        return new SeckillOrderMessage(seckillGoodsId, userId, addressId, retryCount + 1);
    }
}