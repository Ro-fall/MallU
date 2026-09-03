package com.example.mallu.seckill.mq;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderMessageProducer {
    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    void configurePublisherCallbacks() {
        rabbitTemplate.setConfirmCallback((correlation, acknowledged, cause) -> {
            if (!acknowledged) {
                log.error("秒杀订单消息未被 RabbitMQ 确认: correlationId={}, cause={}",
                        correlation == null ? null : correlation.getId(), cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "秒杀订单消息无法路由: exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
    }
    public void publish(Long seckillGoodsId, Long userId, Long addressId) {
        try {
            rabbitTemplate.convertAndSend(SeckillRabbitMqConfig.EXCHANGE, SeckillRabbitMqConfig.ROUTING_KEY,
                    new SeckillOrderMessage(seckillGoodsId, userId, addressId, 0),
                    new CorrelationData(UUID.randomUUID().toString()));
        } catch (AmqpException e) {
            throw new BusinessException(ResultCode.MQ_UNAVAILABLE, "秒杀订单消息投递失败，请稍后重试");
        }
    }
}