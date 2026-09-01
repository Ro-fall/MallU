package com.example.mallu.test.cases;

import com.example.mallu.test.framework.ApiClient;
import com.example.mallu.test.framework.ApiResponse;
import com.example.mallu.test.framework.TestAssertions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 测试基类：提供登录、创建地址、加购物车、获取幂等 Token、优惠券领取等公共前置操作。
 */
public abstract class BaseApiTest {

    protected static final String BASE_URL = System.getProperty("mallu.base.url", "http://localhost:8080");

    protected ApiClient client;

    protected void initClient() {
        client = new ApiClient(BASE_URL);
    }

    protected String login(String username, String password) {
        ApiResponse resp = client.login(username, password);
        TestAssertions.assertSuccess(resp);
        return client.getToken();
    }

    protected String registerAndLogin() {
        String username = "apitest_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123456";
        client.register(username, password, "138" + String.format("%08d", System.currentTimeMillis() % 100000000), null);
        login(username, password);
        return username;
    }

    protected Long createAddress(String receiverName) {
        Map<String, Object> addr = new LinkedHashMap<>();
        addr.put("receiverName", receiverName == null ? "测试收货人" : receiverName);
        addr.put("phone", "13800138000");
        addr.put("province", "浙江省");
        addr.put("city", "杭州市");
        addr.put("district", "西湖区");
        addr.put("detailAddress", "文一西路 123 号");
        addr.put("isDefault", 1);
        ApiResponse resp = client.post("/api/addresses", addr);
        TestAssertions.assertSuccess(resp);
        return resp.field("id").asLong();
    }

    protected void addCart(Long productId, Integer quantity) {
        Map<String, Object> cart = new LinkedHashMap<>();
        cart.put("productId", productId);
        cart.put("quantity", quantity);
        ApiResponse resp = client.post("/api/carts", cart);
        TestAssertions.assertSuccess(resp);
    }

    protected String getIdempotencyKey() {
        ApiResponse resp = client.get("/api/idempotent/token");
        TestAssertions.assertSuccess(resp);
        return resp.dataAsString();
    }
}
