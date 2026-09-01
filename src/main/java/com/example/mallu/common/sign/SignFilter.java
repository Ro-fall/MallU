package com.example.mallu.common.sign;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SignFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final long TIME_WINDOW_MS = 5 * 60 * 1000;

    private static final List<SignPath> SIGN_PATHS = List.of(
            new SignPath("POST", "/api/orders"),
            new SignPath("PUT", "/api/orders/*/pay"),
            new SignPath("POST", "/api/seckill/goods/*/seckill")
    );

    private static final List<String> SKIP_PATHS = List.of(
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/webjars/**"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (isSignRequired(httpRequest)) {
            RepeatableReadHttpServletRequest wrappedRequest = new RepeatableReadHttpServletRequest(httpRequest);
            if (!verifySign(wrappedRequest, (HttpServletResponse) response)) {
                return;
            }
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isSignRequired(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (SKIP_PATHS.stream().anyMatch(p -> PATH_MATCHER.match(p, path))) {
            return false;
        }
        return SIGN_PATHS.stream().anyMatch(p -> p.method.equals(method) && PATH_MATCHER.match(p.pattern, path));
    }

    private boolean verifySign(RepeatableReadHttpServletRequest request, HttpServletResponse response) throws IOException {
        String sign = request.getHeader(SignUtil.SIGN_HEADER);
        String timestampStr = request.getHeader(SignUtil.TIMESTAMP_HEADER);
        String nonce = request.getHeader(SignUtil.NONCE_HEADER);

        String errorMsg = null;
        if (sign == null || timestampStr == null || nonce == null) {
            errorMsg = "缺少签名参数";
        }

        long timestamp = 0;
        if (errorMsg == null) {
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                errorMsg = "时间戳格式错误";
            }
        }

        if (errorMsg == null) {
            long now = System.currentTimeMillis();
            if (Math.abs(now - timestamp) > TIME_WINDOW_MS) {
                errorMsg = "请求时间戳已过期";
            }
        }

        if (errorMsg == null) {
            String nonceKey = "sign:nonce:" + nonce;
            Boolean exists = stringRedisTemplate.hasKey(nonceKey);
            if (Boolean.TRUE.equals(exists)) {
                errorMsg = "请求 nonce 已使用";
            } else {
                stringRedisTemplate.opsForValue().set(nonceKey, "1", TIME_WINDOW_MS, TimeUnit.MILLISECONDS);
            }
        }

        if (errorMsg == null) {
            Map<String, String> params = new TreeMap<>();
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String name = paramNames.nextElement();
                params.put(name, request.getParameter(name));
            }
            String body = request.getBody();
            if (body != null && !body.isBlank()) {
                params.put("body", body.trim());
            }

            String expectedSign = SignUtil.generateSignature(params, timestamp, nonce);
            if (!expectedSign.equalsIgnoreCase(sign)) {
                errorMsg = "签名验证失败";
            }
        }

        if (errorMsg != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            String json = "{\"code\":401,\"message\":\"" + escapeJson(errorMsg) + "\",\"data\":null}";
            response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            return false;
        }

        return true;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record SignPath(String method, String pattern) {
    }
}
