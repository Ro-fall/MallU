package com.example.mallu.seckill.mq;

import com.example.mallu.common.redis.SeckillRedisService;
import com.example.mallu.seckill.service.SeckillOrderTransactionService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderMessageConsumer {
    private static final int MAX_RETRIES = 3;
    private final SeckillOrderTransactionService transactionService;
    private final SeckillRedisService seckillRedisService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = SeckillRabbitMqConfig.QUEUE)
    public void consume(SeckillOrderMessage message, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            transactionService.createOrder(message.seckillGoodsId(), message.userId(), message.addressId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception processingError) {
            retryOrDeadLetter(message, channel, deliveryTag, processingError);
        }
    }

    private void retryOrDeadLetter(SeckillOrderMessage message, Channel channel, long deliveryTag, Exception error)
            throws IOException {
        try {
            if (message.retryCount() < MAX_RETRIES) {
                rabbitTemplate.convertAndSend(SeckillRabbitMqConfig.RETRY_EXCHANGE, SeckillRabbitMqConfig.ROUTING_KEY,
                        message.nextRetry());
                log.warn("秒杀订单消费失败，将第 {} 次重试: goodsId={}, userId={}",
                        message.retryCount() + 1, message.seckillGoodsId(), message.userId(), error);
            } else {
                rabbitTemplate.convertAndSend(SeckillRabbitMqConfig.DEAD_EXCHANGE, SeckillRabbitMqConfig.ROUTING_KEY, message);
                try {
                    seckillRedisService.recoverStock(message.seckillGoodsId(), message.userId());
                    seckillRedisService.markResultFailed(message.seckillGoodsId(), message.userId());
                } catch (RuntimeException redisError) {
                    log.error("秒杀消息已进入死信队列，但 Redis 补偿失败: goodsId={}, userId={}",
                            message.seckillGoodsId(), message.userId(), redisError);
                }
                log.error("秒杀订单最终失败，已进入死信队列: goodsId={}, userId={}",
                        message.seckillGoodsId(), message.userId(), error);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception publishError) {
            log.error("秒杀重试/死信消息投递失败，将重新入队", publishError);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}