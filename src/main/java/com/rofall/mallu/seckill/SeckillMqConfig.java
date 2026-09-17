package com.rofall.mallu.seckill;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeckillMqConfig {
    public static final String MAIN_EXCHANGE = "mallu.seckill.exchange";
    public static final String RETRY_EXCHANGE = "mallu.seckill.retry.exchange";
    public static final String DEAD_EXCHANGE = "mallu.seckill.dead.exchange";
    public static final String MAIN_QUEUE = "mallu.seckill.order.queue";
    public static final String RETRY_QUEUE = "mallu.seckill.retry.queue";
    public static final String DEAD_QUEUE = "mallu.seckill.dead.queue";
    public static final String MAIN_KEY = "seckill.order";
    public static final String RETRY_KEY = "seckill.retry";
    public static final String DEAD_KEY = "seckill.dead";

    @Bean DirectExchange seckillMainExchange() { return new DirectExchange(MAIN_EXCHANGE, true, false); }
    @Bean DirectExchange seckillRetryExchange() { return new DirectExchange(RETRY_EXCHANGE, true, false); }
    @Bean DirectExchange seckillDeadExchange() { return new DirectExchange(DEAD_EXCHANGE, true, false); }
    @Bean Queue seckillMainQueue() { return QueueBuilder.durable(MAIN_QUEUE).withArgument("x-dead-letter-exchange", RETRY_EXCHANGE).withArgument("x-dead-letter-routing-key", RETRY_KEY).build(); }
    @Bean Queue seckillRetryQueue() { return QueueBuilder.durable(RETRY_QUEUE).withArgument("x-message-ttl", 3000).withArgument("x-dead-letter-exchange", MAIN_EXCHANGE).withArgument("x-dead-letter-routing-key", MAIN_KEY).build(); }
    @Bean Queue seckillDeadQueue() { return QueueBuilder.durable(DEAD_QUEUE).build(); }
    @Bean Binding mainBinding() { return BindingBuilder.bind(seckillMainQueue()).to(seckillMainExchange()).with(MAIN_KEY); }
    @Bean Binding retryBinding() { return BindingBuilder.bind(seckillRetryQueue()).to(seckillRetryExchange()).with(RETRY_KEY); }
    @Bean Binding deadBinding() { return BindingBuilder.bind(seckillDeadQueue()).to(seckillDeadExchange()).with(DEAD_KEY); }
    @Bean MessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
}
