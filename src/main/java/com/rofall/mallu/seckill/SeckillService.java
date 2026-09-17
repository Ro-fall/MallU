package com.rofall.mallu.seckill;

import com.rofall.mallu.catalog.ProductRepository;
import com.rofall.mallu.common.BusinessException;
import com.rofall.mallu.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillService {
    private final SeckillActivityRepository activityRepository;
    private final SeckillGoodsRepository goodsRepository;
    private final ProductRepository productRepository;
    private final SeckillRedisService redisService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(readOnly = true)
    public List<SeckillGoodsResponse> list(Long activityId) {
        SeckillActivity activity = activityRepository.findById(activityId).orElseThrow(() -> new BusinessException(4049, HttpStatus.NOT_FOUND, "秒杀活动不存在"));
        return goodsRepository.findByActivityId(activityId).stream().map(goods -> {
            var product = productRepository.findById(goods.getProductId()).orElseThrow(() -> new BusinessException(4042, HttpStatus.NOT_FOUND, "商品不存在"));
            return new SeckillGoodsResponse(goods.getId(), goods.getActivityId(), goods.getProductId(), product.getName(), goods.getSeckillPrice(), goods.getStock(), activity.activeNow());
        }).toList();
    }

    public SeckillResultResponse submit(Long goodsId, SeckillRequest request) {
        Long userId = UserContext.requireUserId();
        SeckillGoods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException(4048, HttpStatus.NOT_FOUND, "秒杀商品不存在"));
        SeckillActivity activity = activityRepository.findById(goods.getActivityId()).orElseThrow(() -> new BusinessException(4049, HttpStatus.NOT_FOUND, "秒杀活动不存在"));
        if (!activity.activeNow()) throw new BusinessException(4099, HttpStatus.CONFLICT, "秒杀活动未开始或已结束");
        redisService.reserve(userId, goodsId);
        try {
            redisService.resultPending(userId, goodsId);
            rabbitTemplate.convertAndSend(SeckillMqConfig.MAIN_EXCHANGE, SeckillMqConfig.MAIN_KEY, new SeckillOrderMessage(userId, goodsId, request.addressId()));
            return new SeckillResultResponse("PENDING", null);
        } catch (RuntimeException exception) {
            redisService.compensate(userId, goodsId);
            throw new BusinessException(8002, HttpStatus.SERVICE_UNAVAILABLE, "秒杀消息投递失败，请稍后重试");
        }
    }

    public SeckillResultResponse result(Long goodsId) { return redisService.result(UserContext.requireUserId(), goodsId); }
}
