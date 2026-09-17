package com.rofall.mallu.reliability;

import com.rofall.mallu.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class OrderRateLimitService {
    private static final long LIMIT_PER_MINUTE = 10;
    private final StringRedisTemplate redisTemplate;
    public OrderRateLimitService(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }
    public void check(Long userId) {
        String key = "mallu:rate:order:" + userId + ':' + (System.currentTimeMillis() / 60_000);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) redisTemplate.expire(key, Duration.ofMinutes(2));
            if (count != null && count > LIMIT_PER_MINUTE) throw new BusinessException(4291, HttpStatus.TOO_MANY_REQUESTS, "下单请求过于频繁");
        } catch (BusinessException exception) { throw exception; }
        catch (RuntimeException exception) { throw new BusinessException(8001, HttpStatus.SERVICE_UNAVAILABLE, "Redis 不可用，请稍后重试"); }
    }
}
