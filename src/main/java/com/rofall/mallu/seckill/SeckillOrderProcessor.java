package com.rofall.mallu.seckill;

import com.rofall.mallu.address.AddressRepository;
import com.rofall.mallu.catalog.Product;
import com.rofall.mallu.catalog.ProductRepository;
import com.rofall.mallu.common.BusinessException;
import com.rofall.mallu.order.MallOrder;
import com.rofall.mallu.order.MallOrderRepository;
import com.rofall.mallu.order.OrderItem;
import com.rofall.mallu.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeckillOrderProcessor {
    private final SeckillOrderRepository seckillOrderRepository;
    private final SeckillGoodsRepository seckillGoodsRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final MallOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Long create(SeckillOrderMessage message) {
        var existing = seckillOrderRepository.findByUserIdAndSeckillGoodsId(message.userId(), message.seckillGoodsId());
        if (existing.isPresent()) return existing.get().getOrderId();
        addressRepository.findByIdAndUserId(message.addressId(), message.userId())
                .orElseThrow(() -> new BusinessException(4043, HttpStatus.NOT_FOUND, "收货地址不存在"));
        SeckillGoods goods = seckillGoodsRepository.findById(message.seckillGoodsId())
                .orElseThrow(() -> new BusinessException(4048, HttpStatus.NOT_FOUND, "秒杀商品不存在"));
        Product product = productRepository.findById(goods.getProductId()).filter(Product::getStatus)
                .orElseThrow(() -> new BusinessException(4042, HttpStatus.NOT_FOUND, "商品不存在或已下架"));
        if (product.getStock() <= 0) throw new BusinessException(4003, HttpStatus.CONFLICT, "商品库存不足");
        goods.decreaseStock();
        product.decreaseStock(1);
        String orderNo = "MS" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MallOrder order = orderRepository.save(new MallOrder(orderNo, message.userId(), message.addressId(), null,
                goods.getSeckillPrice(), BigDecimal.ZERO, goods.getSeckillPrice()));
        orderItemRepository.save(new OrderItem(order.getId(), product.getId(), product.getName(), goods.getSeckillPrice(), 1));
        seckillOrderRepository.save(new SeckillOrder(goods.getId(), order.getId(), message.userId()));
        return order.getId();
    }
}
