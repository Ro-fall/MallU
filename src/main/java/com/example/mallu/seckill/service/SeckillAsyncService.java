package com.example.mallu.seckill.service;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.redis.SeckillRedisService;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.order.entity.Order;
import com.example.mallu.order.entity.OrderItem;
import com.example.mallu.order.mapper.OrderItemMapper;
import com.example.mallu.order.mapper.OrderMapper;
import com.example.mallu.product.entity.Product;
import com.example.mallu.product.mapper.ProductMapper;
import com.example.mallu.seckill.entity.SeckillGoods;
import com.example.mallu.seckill.entity.SeckillOrder;
import com.example.mallu.seckill.mapper.SeckillGoodsMapper;
import com.example.mallu.seckill.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillAsyncService {

    private final ExecutorService virtualThreadExecutor;
    private final SeckillGoodsMapper goodsMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final SeckillRedisService seckillRedisService;

    /**
     * 异步落库：Redis 库存已扣减，这里扣 MySQL 库存并创建订单。
     * 落库失败时恢复 Redis 库存，保证最终一致性。
     */
    public void createOrderAsync(Long seckillGoodsId, Long userId, Long addressId) {
        virtualThreadExecutor.submit(() -> {
            try {
                doCreateOrder(seckillGoodsId, userId, addressId);
            } catch (Exception e) {
                log.error("[秒杀异步] 落库失败, goodsId={}, userId={}", seckillGoodsId, userId, e);
                // 恢复 Redis 库存与用户购买记录，并标记失败
                seckillRedisService.recoverStock(seckillGoodsId, userId);
                seckillRedisService.markResultFailed(seckillGoodsId, userId);
            }
        });
    }

    @Transactional
    public SeckillOrder doCreateOrder(Long seckillGoodsId, Long userId, Long addressId) {
        SeckillGoods goods = goodsMapper.selectById(seckillGoodsId);
        if (goods == null) {
            throw new BusinessException(ResultCode.SECKILL_GOODS_NOT_FOUND);
        }
        Product product = productMapper.selectById(goods.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }

        // 扣减 MySQL 秒杀库存（兜底，防止 Redis 与数据库不一致）
        int affected = goodsMapper.decreaseStock(seckillGoodsId, 1);
        if (affected == 0) {
            throw new BusinessException(ResultCode.SECKILL_STOCK_EMPTY);
        }

        // 扣减商品总库存
        productMapper.decreaseStock(product.getId(), 1);

        // 创建普通订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAddressId(addressId);
        order.setTotalAmount(goods.getSeckillPrice());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(goods.getSeckillPrice());
        order.setStatus(0);
        orderMapper.insert(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setProductImage(product.getMainImage());
        orderItem.setProductPrice(goods.getSeckillPrice());
        orderItem.setQuantity(1);
        orderItem.setTotalPrice(goods.getSeckillPrice());
        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        orderItemMapper.batchInsert(items);

        // 创建秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setSeckillGoodsId(seckillGoodsId);
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setUserId(userId);
        seckillOrder.setStatus(1);
        seckillOrderMapper.insert(seckillOrder);

        // 记录异步结果，供前端轮询
        seckillRedisService.markResultSuccess(seckillGoodsId, userId, seckillOrder.getId());
        log.info("[秒杀异步] 落库成功, seckillOrderId={}, orderId={}", seckillOrder.getId(), order.getId());
        return seckillOrder;
    }

    private String generateOrderNo() {
        return "S" + System.currentTimeMillis() + String.format("%06d", (int) (Math.random() * 1000000));
    }
}
