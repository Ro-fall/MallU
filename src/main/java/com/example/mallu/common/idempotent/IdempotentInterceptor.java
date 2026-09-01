package com.example.mallu.common.idempotent;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final IdempotentTokenService idempotentTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        String token = request.getHeader(IDEMPOTENCY_HEADER);
        log.info("收到幂等请求, userId={}, header={}, token={}", UserContext.getUserId(), IDEMPOTENCY_HEADER, token);
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少幂等 Token: " + IDEMPOTENCY_HEADER);
        }

        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        boolean consumed = idempotentTokenService.consumeToken(userId, token);
        if (!consumed) {
            log.warn("重复请求被拦截, userId={}, token={}", userId, token);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "重复请求，请勿重复提交");
        }

        return true;
    }
}
