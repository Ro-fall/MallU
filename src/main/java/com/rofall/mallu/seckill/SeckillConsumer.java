package com.rofall.mallu.seckill;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class SeckillConsumer {
    private final SeckillOrderProcessor processor;
    private final SeckillRedisService redisService;
    private final RabbitTemplate rabbitTemplate;
    private final AtomicInteger remainingTestFailures = new AtomicInteger();
    private int maxRetries = 3;

    @Value("${mallu.seckill.test-fail-attempts:0}")
    void setTestFailAttempts(int attempts) { remainingTestFailures.set(Math.max(0, attempts)); }

    @Value("${mallu.seckill.max-retries:3}")
    void setMaxRetries(int retries) { maxRetries = Math.max(0, retries); }

    @RabbitListener(queues = SeckillMqConfig.MAIN_QUEUE)
    public void consume(SeckillOrderMessage payload, Message raw, Channel channel) throws IOException {
        long tag = raw.getMessageProperties().getDeliveryTag();
        try {
            if (remainingTestFailures.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
                throw new IllegalStateException("本地秒杀消费者故障注入");
            }
            Long orderId = processor.create(payload);
            redisService.resultSuccess(payload.userId(), payload.seckillGoodsId(), orderId);
            channel.basicAck(tag, false);
        } catch (Exception exception) {
            int retry = raw.getMessageProperties().getHeader("x-retry-count") == null ? 0 : ((Number) raw.getMessageProperties().getHeader("x-retry-count")).intValue();
            if (retry < maxRetries) {
                rabbitTemplate.convertAndSend(SeckillMqConfig.RETRY_EXCHANGE, SeckillMqConfig.RETRY_KEY, payload, message -> {
                    message.getMessageProperties().setHeader("x-retry-count", retry + 1);
                    return message;
                });
            } else {
                rabbitTemplate.convertAndSend(SeckillMqConfig.DEAD_EXCHANGE, SeckillMqConfig.DEAD_KEY, payload);
                redisService.compensate(payload.userId(), payload.seckillGoodsId());
                redisService.resultFailed(payload.userId(), payload.seckillGoodsId());
            }
            channel.basicAck(tag, false);
        }
    }
}
