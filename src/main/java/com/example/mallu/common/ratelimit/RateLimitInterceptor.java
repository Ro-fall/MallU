package com.example.mallu.common.ratelimit;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String key = buildKey(request, rateLimit);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.opsForValue().set(key, "1", rateLimit.windowSeconds(), TimeUnit.SECONDS);
        }

        if (count != null && count > rateLimit.limit()) {
            throw new BusinessException(ResultCode.RATE_LIMIT, "请求过于频繁，请稍后重试");
        }

        return true;
    }

    private String buildKey(HttpServletRequest request, RateLimit rateLimit) {
        String key = RATE_LIMIT_KEY_PREFIX + request.getRequestURI();
        if (rateLimit.scope() == RateLimit.Scope.USER) {
            Long userId = UserContext.getUserId();
            key += ":" + (userId == null ? request.getRemoteAddr() : userId);
        } else {
            key += ":global";
        }
        return key;
    }
}
