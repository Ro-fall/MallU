package com.example.mallu.test.cases;

import com.example.mallu.test.framework.ApiResponse;
import com.example.mallu.test.framework.TestAssertions;
import com.example.mallu.test.framework.TestSignUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单接口 正常/异常/边界 场景测试。
 */
public class OrderNegativeTest extends BaseApiTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setUp() {
        initClient();
    }

    @Test
    @DisplayName("缺少 Idempotency-Key 返回 400")
    void missingIdempotencyKey() {
        registerAndLogin();
        Long addressId = createAddress("王五");
        addCart(1L, 1);

        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("addressId", addressId);
        ApiResponse resp = client.signedPost("/api/orders", client.toJsonString(orderBody), null);
        TestAssertions.assertCode(resp, 400);
        TestAssertions.assertMessageContains(resp, "幂等");
    }

    @Test
    @DisplayName("错误签名返回 401 签名验证失败")
    void badSign() {
        registerAndLogin();
        Long addressId = createAddress("赵六");
        addCart(1L, 1);

        String idempotencyKey = getIdempotencyKey();
        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("addressId", addressId);
        String jsonBody = client.toJsonString(orderBody);

        // 手动构造错误签名请求
        long timestamp = TestSignUtil.currentTimestamp();
        String nonce = TestSignUtil.generateNonce();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(client.getToken());
        headers.set(TestSignUtil.TIMESTAMP_HEADER, String.valueOf(timestamp));
        headers.set(TestSignUtil.NONCE_HEADER, nonce);
        headers.set(TestSignUtil.SIGN_HEADER, "bad-sign-value");
        headers.set("Idempotency-Key", idempotencyKey);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/api/orders", HttpMethod.POST, entity, String.class);
        ApiResponse resp = new ApiResponse(response.getBody());
        TestAssertions.assertCode(resp, 401);
        TestAssertions.assertMessageContains(resp, "签名");
    }

    @Test
    @DisplayName("购物车为空返回 3001")
    void emptyCart() {
        registerAndLogin();
        Long addressId = createAddress("钱七");

        String idempotencyKey = getIdempotencyKey();
        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("addressId", addressId);
        ApiResponse resp = client.signedPost("/api/orders", client.toJsonString(orderBody), idempotencyKey);
        TestAssertions.assertCode(resp, 3001);
        TestAssertions.assertMessageContains(resp, "购物车为空");
    }

    @Test
    @DisplayName("地址不存在返回 2001")
    void addressNotFound() {
        registerAndLogin();
        addCart(1L, 1);
        String idempotencyKey = getIdempotencyKey();

        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("addressId", 99999999L);
        ApiResponse resp = client.signedPost("/api/orders", client.toJsonString(orderBody), idempotencyKey);
        TestAssertions.assertCode(resp, 2001);
    }

    @Test
    @DisplayName("支付回调金额不一致返回 4102")
    void amountNotMatch() {
        registerAndLogin();
        Long addressId = createAddress("孙八");
        addCart(1L, 1);
        String idempotencyKey = getIdempotencyKey();

        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("addressId", addressId);
        ApiResponse createResp = client.signedPost("/api/orders", client.toJsonString(orderBody), idempotencyKey);
        TestAssertions.assertSuccess(createResp);

        String orderNo = createResp.field("orderNo").asText();
        Map<String, Object> callback = new LinkedHashMap<>();
        callback.put("orderNo", orderNo);
        callback.put("payAmount", 0.01);
        callback.put("tradeNo", "TRADE-BADAMT" + System.currentTimeMillis());
        callback.put("result", "SUCCESS");

        ApiResponse payResp = client.post("/api/payments/callback", callback, false);
        TestAssertions.assertCode(payResp, 4102);
        TestAssertions.assertMessageContains(payResp, "金额");
    }
}
