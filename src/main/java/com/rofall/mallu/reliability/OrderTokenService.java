package com.rofall.mallu.reliability;

import com.rofall.mallu.common.BusinessException;
import com.rofall.mallu.security.UserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class OrderTokenService {
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); " +
            "if not value then return 0; end; " +
            "if value ~= ARGV[1] then return -1; end; " +
            "redis.call('DEL', KEYS[1]); return 1;", Long.class);
    private final StringRedisTemplate redisTemplate;
    public OrderTokenService(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }

    public OrderTokenResponse create(OrderTokenRequest request) {
        Long userId = UserContext.requireUserId();
        if (request.cartItemIds().stream().distinct().count() != request.cartItemIds().size()) {
            throw new BusinessException(4004, HttpStatus.BAD_REQUEST, "购物车项不能重复");
        }
        String binding = binding(request.cartItemIds());
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(key(userId, token), binding, TTL);
        } catch (RuntimeException exception) { throw unavailable(); }
        return new OrderTokenResponse(token, Instant.now().plus(TTL));
    }

    public void consume(Long userId, String token, List<Long> cartItemIds) {
        Long result;
        try { result = redisTemplate.execute(CONSUME_SCRIPT, List.of(key(userId, token)), binding(cartItemIds)); }
        catch (RuntimeException exception) { throw unavailable(); }
        if (result == null || result == 0) throw new BusinessException(4096, HttpStatus.CONFLICT, "下单 Token 无效、已使用或已过期");
        if (result == -1) throw new BusinessException(4097, HttpStatus.CONFLICT, "下单 Token 与购物车项不匹配");
    }

    private String key(Long userId, String token) { return "mallu:order:token:" + userId + ':' + token; }
    private String binding(List<Long> ids) { return ids.stream().distinct().sorted().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""); }
    private BusinessException unavailable() { return new BusinessException(8001, HttpStatus.SERVICE_UNAVAILABLE, "Redis 不可用，请稍后重试"); }
}
