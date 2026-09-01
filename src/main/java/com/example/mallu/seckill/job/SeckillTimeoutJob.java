package com.example.mallu.seckill.job;

import com.example.mallu.seckill.entity.SeckillOrder;
import com.example.mallu.seckill.mapper.SeckillOrderMapper;
import com.example.mallu.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillTimeoutJob {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillService seckillService;

    private static final Integer TIMEOUT_MINUTES = 5;

    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        List<SeckillOrder> timeoutOrders = seckillOrderMapper.selectTimeoutOrders(TIMEOUT_MINUTES);
        if (timeoutOrders.isEmpty()) {
            return;
        }
        log.info("扫描到 {} 个超时未支付秒杀订单", timeoutOrders.size());
        for (SeckillOrder order : timeoutOrders) {
            try {
                seckillService.cancelTimeoutSeckillOrder(order.getId());
                log.info("已取消超时秒杀订单: seckillOrderId={}", order.getId());
            } catch (Exception e) {
                log.error("取消超时秒杀订单失败: seckillOrderId={}", order.getId(), e);
            }
        }
    }
}
