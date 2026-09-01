package com.example.mallu.common.idempotent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

    public String generateToken(Long userId, int expireSeconds) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = IDEMPOTENT_KEY_PREFIX + userId + ":" + token;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(success)) {
            throw new RuntimeException("生成幂等 Token 失败，key 已存在: " + key);
        }
        log.info("生成幂等 Token: {}, userId={}", key, userId);
        return token;
    }

    public boolean consumeToken(Long userId, String token) {
        String key = IDEMPOTENT_KEY_PREFIX + userId + ":" + token;
        Boolean exists = redisTemplate.hasKey(key);
        Boolean deleted = redisTemplate.delete(key);
        log.info("消费幂等 Token: {}, exists={}, deleted={}", key, exists, deleted);
        return Boolean.TRUE.equals(deleted);
    }
}
