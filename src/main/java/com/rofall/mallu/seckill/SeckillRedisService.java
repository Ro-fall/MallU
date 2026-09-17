package com.rofall.mallu.seckill;

import com.rofall.mallu.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Service
public class SeckillRedisService {
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>(
            "local stock=redis.call('GET',KEYS[1]); if not stock then return -3 end; if redis.call('EXISTS',KEYS[2])==1 then return -2 end; if tonumber(stock)<=0 then return -1 end; redis.call('DECR',KEYS[1]); redis.call('SET',KEYS[2],'1','EX',ARGV[1]); return 1;", Long.class);
    private static final DefaultRedisScript<Long> COMPENSATE = new DefaultRedisScript<>(
            "redis.call('INCR',KEYS[1]); redis.call('DEL',KEYS[2]); return 1;", Long.class);
    private final StringRedisTemplate redis;
    public SeckillRedisService(StringRedisTemplate redis) { this.redis = redis; }
    public void initializeIfAbsent(SeckillGoods goods) {
        try { redis.opsForValue().setIfAbsent(stockKey(goods.getId()), String.valueOf(goods.getStock())); }
        catch (RuntimeException ignored) { }
    }
    public void reserve(Long userId, Long goodsId) {
        Long result; try { result=redis.execute(RESERVE, List.of(stockKey(goodsId), userKey(goodsId,userId)), String.valueOf(Duration.ofHours(2).toSeconds())); } catch (RuntimeException ex) { throw unavailable(); }
        if (result == null || result == -3) throw unavailable();
        if (result == -2) throw new BusinessException(4098, HttpStatus.CONFLICT, "每位用户只能秒杀一次");
        if (result == -1) throw new BusinessException(4003, HttpStatus.CONFLICT, "秒杀库存不足");
    }
    public void compensate(Long userId, Long goodsId) { try { redis.execute(COMPENSATE, List.of(stockKey(goodsId), userKey(goodsId,userId))); } catch (RuntimeException ignored) { } }
    public void resultPending(Long userId, Long goodsId) { setResult(userId,goodsId,"PENDING"); }
    public void resultSuccess(Long userId, Long goodsId, Long orderId) { setResult(userId,goodsId,"SUCCESS:"+orderId); }
    public void resultFailed(Long userId, Long goodsId) { setResult(userId,goodsId,"FAILED"); }
    public SeckillResultResponse result(Long userId, Long goodsId) { try { String raw=redis.opsForValue().get(resultKey(goodsId,userId)); if(raw==null) return new SeckillResultResponse("NONE",null); if(raw.startsWith("SUCCESS:")) return new SeckillResultResponse("SUCCESS",Long.valueOf(raw.substring(8))); return new SeckillResultResponse(raw,null); } catch(RuntimeException ex) { throw unavailable(); } }
    private void setResult(Long userId,Long goodsId,String value) { try { redis.opsForValue().set(resultKey(goodsId,userId),value,Duration.ofHours(2)); } catch(RuntimeException ex) { throw unavailable(); } }
    private String stockKey(Long goodsId){return "mallu:seckill:stock:"+goodsId;} private String userKey(Long goodsId,Long userId){return "mallu:seckill:user:"+goodsId+':'+userId;} private String resultKey(Long goodsId,Long userId){return "mallu:seckill:result:"+goodsId+':'+userId;}
    private BusinessException unavailable(){return new BusinessException(8001,HttpStatus.SERVICE_UNAVAILABLE,"Redis 不可用，请稍后重试");}
}
