package com.example.mallu.test.framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一 API 客户端：封装认证、签名、幂等 Token、请求发送。
 * 测试数据与脚本分离：所有请求参数由调用方传入，本类只负责协议层。
 */
public class ApiClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    private String token;
    private String username;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    // ---------- 认证 ----------

    public ApiResponse register(String username, String password, String phone, String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        if (phone != null) body.put("phone", phone);
        if (email != null) body.put("email", email);
        return send("POST", "/api/users/register", null, null, body, false);
    }

    public ApiResponse login(String username, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        ApiResponse resp = send("POST", "/api/users/login", null, null, body, false);
        if (resp.success()) {
            this.token = resp.data().path("token").asText();
            this.username = username;
        }
        return resp;
    }

    // ---------- 通用请求 ----------

    public ApiResponse get(String path) {
        return send("GET", path, null, null, null, true);
    }

    public ApiResponse getWithQuery(String path, Map<String, String> query) {
        return send("GET", path, query, null, null, true);
    }

    public ApiResponse post(String path, Object body) {
        return post(path, body, true);
    }

    public ApiResponse post(String path, Object body, boolean auth) {
        return send("POST", path, null, null, body, auth);
    }

    public ApiResponse put(String path, Object body) {
        return send("PUT", path, null, null, body, true);
    }

    public ApiResponse delete(String path) {
        return send("DELETE", path, null, null, null, true);
    }

    /**
     * 签名请求：自动生成 X-Timestamp / X-Nonce / X-Sign（可选 Idempotency-Key）
     */
    public ApiResponse signedPost(String path, String jsonBody, String idempotencyKey) {
        return signedSend("POST", path, jsonBody, idempotencyKey);
    }

    public ApiResponse signedPostWithQuery(String path, Map<String, String> query) {
        Map<String, String> signParams = new LinkedHashMap<>(query);
        long timestamp = TestSignUtil.currentTimestamp();
        String nonce = TestSignUtil.generateNonce();
        String sign = TestSignUtil.generateSignature(signParams, timestamp, nonce);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set(TestSignUtil.TIMESTAMP_HEADER, String.valueOf(timestamp));
        headers.set(TestSignUtil.NONCE_HEADER, nonce);
        headers.set(TestSignUtil.SIGN_HEADER, sign);
        return send("POST", path, query, headers.toSingleValueMap(), null, true);
    }

    public ApiResponse signedPut(String path, String jsonBody, String idempotencyKey) {
        return signedSend("PUT", path, jsonBody, idempotencyKey);
    }

    private ApiResponse signedSend(String method, String path, String jsonBody, String idempotencyKey) {
        Map<String, String> signParams = new LinkedHashMap<>();
        if (jsonBody != null && !jsonBody.isBlank()) {
            signParams.put("body", jsonBody.trim());
        }
        long timestamp = TestSignUtil.currentTimestamp();
        String nonce = TestSignUtil.generateNonce();
        String sign = TestSignUtil.generateSignature(signParams, timestamp, nonce);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.set(TestSignUtil.TIMESTAMP_HEADER, String.valueOf(timestamp));
        headers.set(TestSignUtil.NONCE_HEADER, nonce);
        headers.set(TestSignUtil.SIGN_HEADER, sign);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            headers.set("Idempotency-Key", idempotencyKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(baseUrl + path,
                HttpMethod.valueOf(method), entity, String.class);
        return new ApiResponse(response.getBody());
    }

    private ApiResponse send(String method, String path, Map<String, String> query,
                             Map<String, String> extraHeaders, Object body, boolean auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (auth && token != null) {
            headers.setBearerAuth(token);
        }
        if (extraHeaders != null) {
            extraHeaders.forEach(headers::set);
        }

        String url = baseUrl + path;
        if (query != null && !query.isEmpty()) {
            StringBuilder qs = new StringBuilder(url.contains("?") ? "&" : "?");
            query.forEach((k, v) -> qs.append(k).append("=").append(v).append("&"));
            url += qs.substring(0, qs.length() - 1);
        }

        HttpEntity<String> entity = new HttpEntity<>(body == null ? null : toJson(body), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.valueOf(method), entity, String.class);
        return new ApiResponse(response.getBody());
    }

    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    public String toJsonString(Object obj) {
        return toJson(obj);
    }

    public JsonNode parseJson(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
    }
}
