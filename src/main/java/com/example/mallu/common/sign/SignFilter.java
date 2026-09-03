package com.example.mallu.common.sign;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SignFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;
    private final RequestSignService requestSignService;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final long TIME_WINDOW_MS = 5 * 60 * 1000;
    private static final List<SignPath> SIGN_PATHS = List.of(
            new SignPath("POST", "/api/orders"),
            new SignPath("PUT", "/api/orders/*/pay"),
            new SignPath("POST", "/api/seckill/goods/*/seckill")
    );
    private static final List<String> SKIP_PATHS = List.of(
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs.yaml", "/webjars/**"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        try {
            if (!isSignRequired(httpRequest)) {
                chain.doFilter(request, response);
                return;
            }
            RepeatableReadHttpServletRequest wrappedRequest = new RepeatableReadHttpServletRequest(httpRequest);
            if (verifySign(wrappedRequest, (HttpServletResponse) response)) {
                chain.doFilter(wrappedRequest, response);
            }
        } catch (RedisConnectionFailureException e) {
            writeError((HttpServletResponse) response, 8001, "签名服务暂不可用，请稍后重试");
        }
    }

    private boolean isSignRequired(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (SKIP_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path))) {
            return false;
        }
        return SIGN_PATHS.stream().anyMatch(signPath -> signPath.method.equals(method)
                && PATH_MATCHER.match(signPath.pattern, path));
    }

    private boolean verifySign(RepeatableReadHttpServletRequest request, HttpServletResponse response) throws IOException {
        String sign = request.getHeader(SignUtil.SIGN_HEADER);
        String timestampText = request.getHeader(SignUtil.TIMESTAMP_HEADER);
        String nonce = request.getHeader(SignUtil.NONCE_HEADER);
        String errorMessage = null;
        long timestamp = 0;

        if (sign == null || timestampText == null || nonce == null) {
            errorMessage = "缺少签名参数";
        }
        if (errorMessage == null) {
            try {
                timestamp = Long.parseLong(timestampText);
            } catch (NumberFormatException e) {
                errorMessage = "时间戳格式错误";
            }
        }
        if (errorMessage == null && Math.abs(System.currentTimeMillis() - timestamp) > TIME_WINDOW_MS) {
            errorMessage = "请求时间戳已过期";
        }
        if (errorMessage == null) {
            String nonceKey = "sign:nonce:" + nonce;
            // SET NX + TTL 是单条原子命令，多个并发请求只能有一个占用该 nonce。
            Boolean accepted = stringRedisTemplate.opsForValue()
                    .setIfAbsent(nonceKey, "1", TIME_WINDOW_MS, TimeUnit.MILLISECONDS);
            if (!Boolean.TRUE.equals(accepted)) {
                errorMessage = "请求 nonce 已使用";
            }
        }
        if (errorMessage == null) {
            Map<String, String> params = new TreeMap<>();
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                params.put(name, request.getParameter(name));
            }
            String body = request.getBody();
            if (body != null && !body.isBlank()) {
                params.put("body", body.trim());
            }
            String expectedSign = requestSignService.generateSignature(params, timestamp, nonce);
            if (!expectedSign.equalsIgnoreCase(sign)) {
                errorMessage = "签名验证失败";
            }
        }
        if (errorMessage != null) {
            writeError(response, 401, errorMessage);
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"code\":" + code + ",\"message\":\"" + escapeJson(message) + "\",\"data\":null}";
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\b", "\\b").replace("\f", "\\f")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private record SignPath(String method, String pattern) {
    }
}