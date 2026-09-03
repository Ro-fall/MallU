package com.example.mallu.common.health;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAvailabilityService {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isAvailable() {
        try {
            return "PONG".equalsIgnoreCase(stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping()));
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 不可用: {}", e.getMostSpecificCause().getMessage());
            return false;
        } catch (RuntimeException e) {
            log.warn("Redis 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    public void requireAvailable(String operation) {
        if (!isAvailable()) {
            throw new BusinessException(ResultCode.REDIS_UNAVAILABLE, operation + "暂不可用，请稍后重试");
        }
    }
}