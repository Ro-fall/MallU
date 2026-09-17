package com.rofall.mallu.reliability;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Service;

@Service
public class RedisHealthService {
    private final StringRedisTemplate redisTemplate;
    public RedisHealthService(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }
    public RedisHealthResponse check() {
        try {
            String pong;
            try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
                pong = connection.ping();
            }
            return new RedisHealthResponse("PONG".equalsIgnoreCase(pong), pong);
        } catch (RuntimeException exception) {
            return new RedisHealthResponse(false, "UNAVAILABLE");
        }
    }
}
