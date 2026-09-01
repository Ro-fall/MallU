package com.example.mallu.test.cases;

import com.example.mallu.test.framework.ApiResponse;
import com.example.mallu.test.framework.TestAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** 覆盖优惠券回滚和取消秒杀后重新参与的两条历史高风险链路。 */
public class CouponAndSeckillRegressionTest extends BaseApiTest {

    @BeforeEach
    void setUp() {
        initClient();
    }

    @Test
    @DisplayName("优惠券：领取→下单抵扣→取消订单后恢复可用")
    void couponIsRestoredAfterOrderCancellation() {
        registerAndLogin();
        Long addressId = createAddress("优惠券测试");

        ApiResponse availableCoupons = client.get("/api/coupons");
        TestAssertions.assertSuccess(availableCoupons);
        Long couponId = availableCoupons.data().get(0).path("id").asLong();

        ApiResponse claim = client.post("/api/coupons/" + couponId + "/claim", null);
        TestAssertions.assertSuccess(claim);
        Long userCouponId = claim.field("id").asLong();

        addCart(1L, 1);
        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("addressId", addressId);
        orderBody.put("couponId", userCouponId);
        ApiResponse create = client.signedPost("/api/orders", client.toJsonString(orderBody), getIdempotencyKey());
        TestAssertions.assertSuccess(create);
        TestAssertions.assertDataFieldEquals(create, "couponId", userCouponId);

        ApiResponse cancel = client.put("/api/orders/" + create.field("id").asLong() + "/cancel", Map.of());
        TestAssertions.assertSuccess(cancel);
        TestAssertions.assertDataFieldEquals(cancel, "status", 2);

        ApiResponse myAvailable = client.get("/api/coupons/my/available");
        TestAssertions.assertSuccess(myAvailable);
        boolean restored = false;
        for (var coupon : myAvailable.data()) {
            if (coupon.path("id").asLong() == userCouponId) {
                restored = true;
                break;
            }
        }
        if (!restored) {
            throw new AssertionError("取消订单后，已使用优惠券未恢复为可用状态");
        }
    }

    @Test
    @DisplayName("秒杀：取消订单后释放资格，等待限流窗口后可再次参与")
    void canRejoinSeckillAfterCancellation() throws InterruptedException {
        registerAndLogin();
        Long addressId = createAddress("秒杀测试");

        ApiResponse activities = client.get("/api/seckill/activities");
        TestAssertions.assertSuccess(activities);
        Long activityId = activities.data().get(0).path("id").asLong();
        ApiResponse goods = client.get("/api/seckill/activities/" + activityId + "/goods");
        TestAssertions.assertSuccess(goods);
        Long seckillGoodsId = goods.data().get(0).path("id").asLong();

        Long firstOrderId = createAndWaitForSeckillOrder(seckillGoodsId, addressId);
        ApiResponse cancel = client.put("/api/orders/" + firstOrderId + "/cancel", Map.of());
        TestAssertions.assertSuccess(cancel);
        TestAssertions.assertDataFieldEquals(cancel, "status", 2);

        // 接口按用户限流为 3 秒；等待后验证取消确实释放了数据库与 Redis 的秒杀资格。
        Thread.sleep(3_100);
        Long secondOrderId = createAndWaitForSeckillOrder(seckillGoodsId, addressId);
        assertNotEquals(firstOrderId, secondOrderId, "取消后重新秒杀应创建新订单");

        // 清理测试订单，归还有限的秒杀库存。
        TestAssertions.assertSuccess(client.put("/api/orders/" + secondOrderId + "/cancel", Map.of()));
    }

    private Long createAndWaitForSeckillOrder(Long seckillGoodsId, Long addressId) throws InterruptedException {
        ApiResponse seckill = client.signedPostWithQuery("/api/seckill/goods/" + seckillGoodsId + "/seckill",
                Map.of("addressId", String.valueOf(addressId)));
        TestAssertions.assertSuccess(seckill);

        for (int i = 0; i < 20; i++) {
            Thread.sleep(250);
            ApiResponse result = client.get("/api/seckill/result/" + seckillGoodsId);
            TestAssertions.assertSuccess(result);
            if (result.data().path("orderId").canConvertToLong()) {
                return result.data().path("orderId").asLong();
            }
        }
        throw new AssertionError("秒杀异步订单在 5 秒内未创建完成");
    }
}
