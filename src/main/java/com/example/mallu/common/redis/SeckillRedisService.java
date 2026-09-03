package com.example.mallu.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SeckillRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_KEY_PREFIX = "seckill:users:";
    private static final String RESULT_KEY_PREFIX = "seckill:result:";
    private static final long RESULT_KEY_EXPIRE_MINUTES = 10;
    private static final String RESULT_PROCESSING = "0";
    private static final String RESULT_FAILED = "-1";

    private static final String STOCK_LUA_SCRIPT =
            "local stockKey = KEYS[1] " +
            "local userKey = KEYS[2] " +
            "local userId = ARGV[1] " +
            "if redis.call('sismember', userKey, userId) == 1 then return -2 end " +
            "local stock = tonumber(redis.call('get', stockKey)) " +
            "if stock == nil then return -3 end " +
            "if stock <= 0 then return -1 end " +
            "redis.call('decr', stockKey) " +
            "redis.call('sadd', userKey, userId) " +
            "return 1";

    public void preloadStock(Long seckillGoodsId, Integer stock) {
        String key = STOCK_KEY_PREFIX + seckillGoodsId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
    }

    public Long seckill(Long seckillGoodsId, Long userId) {
        String stockKey = STOCK_KEY_PREFIX + seckillGoodsId;
        String userKey = USER_KEY_PREFIX + seckillGoodsId;
        List<String> keys = List.of(stockKey, userKey);

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(STOCK_LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        return stringRedisTemplate.execute(redisScript, keys, String.valueOf(userId));
    }

    public Integer getRemainingStock(Long seckillGoodsId) {
        try {
            String key = STOCK_KEY_PREFIX + seckillGoodsId;
            String value = stringRedisTemplate.opsForValue().get(key);
            return value == null ? null : Integer.parseInt(value);
        } catch (RuntimeException e) {
            // 秒杀展示、详情查询可以退回到 MySQL；下单入口会先执行 Redis 可用性检查。
            return null;
        }
    }

    public void recoverStock(Long seckillGoodsId, Long userId) {
        String stockKey = STOCK_KEY_PREFIX + seckillGoodsId;
        String userKey = USER_KEY_PREFIX + seckillGoodsId;
        stringRedisTemplate.opsForValue().increment(stockKey);
        stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(userId));
    }

    public void clearStock(Long seckillGoodsId) {
        stringRedisTemplate.delete(STOCK_KEY_PREFIX + seckillGoodsId);
        stringRedisTemplate.delete(USER_KEY_PREFIX + seckillGoodsId);
    }

    public void markResultProcessing(Long seckillGoodsId, Long userId) {
        String key = RESULT_KEY_PREFIX + seckillGoodsId + ":" + userId;
        stringRedisTemplate.opsForValue().set(key, RESULT_PROCESSING, RESULT_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    public void markResultSuccess(Long seckillGoodsId, Long userId, Long seckillOrderId) {
        String key = RESULT_KEY_PREFIX + seckillGoodsId + ":" + userId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(seckillOrderId), RESULT_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    public void markResultFailed(Long seckillGoodsId, Long userId) {
        String key = RESULT_KEY_PREFIX + seckillGoodsId + ":" + userId;
        stringRedisTemplate.opsForValue().set(key, RESULT_FAILED, RESULT_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 查询异步秒杀结果：null 表示还未开始处理；0 表示处理中；-1 表示失败；大于 0 为 seckillOrderId
     */
    public Long getResult(Long seckillGoodsId, Long userId) {
        try {
            String key = RESULT_KEY_PREFIX + seckillGoodsId + ":" + userId;
            String value = stringRedisTemplate.opsForValue().get(key);
            return value == null ? null : Long.parseLong(value);
        } catch (RuntimeException e) {
            return null;
        }
    }
    public void rebuildStock(Long seckillGoodsId, Integer stock, List<Long> purchasedUserIds) {
        clearStock(seckillGoodsId);
        preloadStock(seckillGoodsId, stock);
        if (!purchasedUserIds.isEmpty()) {
            stringRedisTemplate.opsForSet().add(USER_KEY_PREFIX + seckillGoodsId,
                    purchasedUserIds.stream().map(String::valueOf).toArray(String[]::new));
        }
    }
}
