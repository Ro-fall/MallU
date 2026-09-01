package com.example.mallu.idempotent.controller;

import com.example.mallu.common.idempotent.IdempotentTokenService;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/idempotent")
@RequiredArgsConstructor
public class IdempotentController {

    private final IdempotentTokenService idempotentTokenService;

    @GetMapping("/token")
    public Result<String> getToken() {
        String token = idempotentTokenService.generateToken(UserContext.getUserId(), 300);
        log.info("返回幂等 Token: {}", token);
        return Result.success(token);
    }
}
