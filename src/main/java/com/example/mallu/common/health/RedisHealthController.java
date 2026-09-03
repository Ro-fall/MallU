package com.example.mallu.common.health;

import com.example.mallu.common.interceptor.SkipAuth;
import com.example.mallu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@SkipAuth
public class RedisHealthController {

    private final RedisAvailabilityService redisAvailabilityService;

    @GetMapping("/redis")
    public Result<Map<String, String>> redis() {
        boolean available = redisAvailabilityService.isAvailable();
        return Result.success(Map.of("redis", available ? "UP" : "DOWN"));
    }
}