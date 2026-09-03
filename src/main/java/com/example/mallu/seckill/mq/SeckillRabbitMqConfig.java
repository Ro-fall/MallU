package com.example.mallu.seckill.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SeckillRabbitMqConfig {
    public static final String EXCHANGE = "mallu.seckill.exchange";
    public static final String RETRY_EXCHANGE = "mallu.seckill.retry.exchange";
    public static final String DEAD_EXCHANGE = "mallu.seckill.dead.exchange";
    public static final String ROUTING_KEY = "seckill.order.create";
    public static final String QUEUE = "mallu.seckill.order.queue";
    public static final String RETRY_QUEUE = "mallu.seckill.order.retry.queue";
    public static final String DEAD_QUEUE = "mallu.seckill.order.dead.queue";

    @Bean DirectExchange seckillExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean DirectExchange seckillRetryExchange() { return new DirectExchange(RETRY_EXCHANGE, true, false); }
    @Bean DirectExchange seckillDeadExchange() { return new DirectExchange(DEAD_EXCHANGE, true, false); }
    @Bean Queue seckillOrderQueue() { return new Queue(QUEUE, true); }
    @Bean Queue seckillRetryQueue() { return new Queue(RETRY_QUEUE, true, false, false, Map.of("x-message-ttl", 5000, "x-dead-letter-exchange", EXCHANGE, "x-dead-letter-routing-key", ROUTING_KEY)); }
    @Bean Queue seckillDeadQueue() { return new Queue(DEAD_QUEUE, true); }
    @Bean Binding seckillOrderBinding() { return BindingBuilder.bind(seckillOrderQueue()).to(seckillExchange()).with(ROUTING_KEY); }
    @Bean Binding seckillRetryBinding() { return BindingBuilder.bind(seckillRetryQueue()).to(seckillRetryExchange()).with(ROUTING_KEY); }
    @Bean Binding seckillDeadBinding() { return BindingBuilder.bind(seckillDeadQueue()).to(seckillDeadExchange()).with(ROUTING_KEY); }
    @Bean MessageConverter rabbitMessageConverter() { return new Jackson2JsonMessageConverter(); }
}