package com.rofall.mallu.order;

import com.rofall.mallu.catalog.ProductRepository;
import com.rofall.mallu.coupon.UserCouponRepository;
import com.rofall.mallu.seckill.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {
    private final MallOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;
    private final SeckillOrderRepository seckillOrderRepository;
    private final SeckillGoodsRepository seckillGoodsRepository;
    private final SeckillRedisService seckillRedisService;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredOrders() {
        for (MallOrder order : orderRepository.findByStatusAndExpiresAtBefore("PENDING_PAYMENT", LocalDateTime.now())) {
            orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).forEach(item -> productRepository.findById(item.getProductId()).ifPresent(product -> product.increaseStock(item.getQuantity())));
            if (order.getUserCouponId() != null) userCouponRepository.findById(order.getUserCouponId()).ifPresent(coupon -> coupon.release());
            seckillOrderRepository.findByOrderId(order.getId()).ifPresent(seckillOrder -> {
                seckillGoodsRepository.findById(seckillOrder.getSeckillGoodsId()).ifPresent(goods -> goods.increaseStock());
                seckillOrder.cancel();
                seckillRedisService.compensate(seckillOrder.getUserId(), seckillOrder.getSeckillGoodsId());
            });
            order.cancel();
        }
    }
}
