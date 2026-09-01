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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 异步线程通过另一个 Spring Bean 调用该方法，确保 @Transactional 能被代理拦截。
 */
@Service
@RequiredArgsConstructor
public class SeckillOrderTransactionService {
    private final SeckillGoodsMapper goodsMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final SeckillRedisService seckillRedisService;

    @Transactional
    public SeckillOrder createOrder(Long seckillGoodsId, Long userId, Long addressId) {
        SeckillGoods goods = goodsMapper.selectById(seckillGoodsId);
        if (goods == null) {
            throw new BusinessException(ResultCode.SECKILL_GOODS_NOT_FOUND);
        }
        Product product = productMapper.selectById(goods.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }
        if (goodsMapper.decreaseStock(seckillGoodsId, 1) == 0) {
            throw new BusinessException(ResultCode.SECKILL_STOCK_EMPTY);
        }
        if (productMapper.decreaseStock(product.getId(), 1) == 0) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }

        Order order = new Order();
        order.setOrderNo("S" + System.currentTimeMillis() + String.format("%06d", (int) (Math.random() * 1_000_000)));
        order.setUserId(userId);
        order.setAddressId(addressId);
        order.setTotalAmount(goods.getSeckillPrice());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(goods.getSeckillPrice());
        order.setStatus(0);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductImage(product.getMainImage());
        item.setProductPrice(goods.getSeckillPrice());
        item.setQuantity(1);
        item.setTotalPrice(goods.getSeckillPrice());
        orderItemMapper.batchInsert(List.of(item));

        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setSeckillGoodsId(seckillGoodsId);
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setUserId(userId);
        seckillOrder.setStatus(1);
        seckillOrderMapper.insert(seckillOrder);
        seckillRedisService.markResultSuccess(seckillGoodsId, userId, seckillOrder.getId());
        return seckillOrder;
    }
}
