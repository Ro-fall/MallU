package com.example.mallu.test.cases;

import com.example.mallu.test.framework.ApiResponse;
import com.example.mallu.test.framework.TestAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 注册 - 登录 - 创建地址 - 加购物车 - 下单 - 支付 - 支付回调 全链路自动化测试。
 */
public class OrderFullFlowTest extends BaseApiTest {

    @BeforeEach
    void setUp() {
        initClient();
    }

    @Test
    @DisplayName("全链路：注册→登录→地址→加购→下单→支付→回调")
    void fullFlow() {
        // 1. 注册并登录
        String username = registerAndLogin();
        assertEquals(username, client.getUsername());

        // 2. 创建地址
        Long addressId = createAddress("张三");
        System.out.println("addressId = " + addressId);

        // 3. 加购物车（商品1 = iPhone 15 Pro）
        addCart(1L, 1);

        // 4. 获取幂等 Token
        String idempotencyKey = getIdempotencyKey();

        // 5. 构造订单 body 并生成签名下单
        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("addressId", addressId);
        String jsonBody = client.toJsonString(orderBody);

        ApiResponse createResp = client.signedPost("/api/orders", jsonBody, idempotencyKey);
        TestAssertions.assertSuccess(createResp);
        String orderNo = createResp.field("orderNo").asText();
        double payAmount = createResp.field("payAmount").asDouble();
        System.out.println("orderNo = " + orderNo + ", payAmount = " + payAmount);

        // 6. 模拟支付平台回调
        Map<String, Object> callback = new LinkedHashMap<>();
        callback.put("orderNo", orderNo);
        callback.put("payAmount", payAmount);
        callback.put("tradeNo", "TRADE" + System.currentTimeMillis());
        callback.put("result", "SUCCESS");

        ApiResponse payResp = client.post("/api/payments/callback", callback, false);
        TestAssertions.assertSuccess(payResp);
        TestAssertions.assertDataFieldEquals(payResp, "success", true);

        // 7. 查询订单确认已支付
        ApiResponse detailResp = client.get("/api/orders/" + createResp.field("id").asLong());
        TestAssertions.assertSuccess(detailResp);
        TestAssertions.assertDataFieldEquals(detailResp, "status", 1);
    }

    @Test
    @DisplayName("支付回调幂等：同一 tradeNo 重复回调返回首次结果")
    void callbackIdempotent() {
        String username = registerAndLogin();
        Long addressId = createAddress("李四");
        addCart(1L, 1);
        String idempotencyKey = getIdempotencyKey();

        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("addressId", addressId);
        ApiResponse createResp = client.signedPost("/api/orders", client.toJsonString(orderBody), idempotencyKey);
        TestAssertions.assertSuccess(createResp);

        String orderNo = createResp.field("orderNo").asText();
        double payAmount = createResp.field("payAmount").asDouble();
        String tradeNo = "TRADE-IDEMP" + System.currentTimeMillis();

        // 第一次回调成功
        Map<String, Object> callback1 = new LinkedHashMap<>();
        callback1.put("orderNo", orderNo);
        callback1.put("payAmount", payAmount);
        callback1.put("tradeNo", tradeNo);
        callback1.put("result", "SUCCESS");
        TestAssertions.assertSuccess(client.post("/api/payments/callback", callback1, false));

        // 第二次回调：支付平台的重试应该正常确认，而不是返回业务错误。
        ApiResponse callback2 = client.post("/api/payments/callback", callback1, false);
        TestAssertions.assertSuccess(callback2);
        TestAssertions.assertDataFieldEquals(callback2, "success", true);
        TestAssertions.assertDataFieldEquals(callback2, "idempotent", true);
    }
}
