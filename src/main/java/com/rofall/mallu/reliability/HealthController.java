package com.rofall.mallu.reliability;

import com.rofall.mallu.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    private final RedisHealthService redisHealthService;
    @GetMapping("/redis")
    public ApiResponse<RedisHealthResponse> redis() { return ApiResponse.success(redisHealthService.check()); }
}
