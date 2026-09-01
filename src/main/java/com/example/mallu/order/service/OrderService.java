package com.example.mallu.order.service;

import com.example.mallu.cart.dto.CartVO;
import com.example.mallu.cart.mapper.CartMapper;
import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.result.PageResult;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.coupon.entity.Coupon;
import com.example.mallu.coupon.entity.UserCoupon;
import com.example.mallu.coupon.mapper.CouponMapper;
import com.example.mallu.coupon.mapper.UserCouponMapper;
import com.example.mallu.coupon.strategy.CouponStrategy;
import com.example.mallu.coupon.strategy.CouponStrategyFactory;
import com.example.mallu.order.dto.OrderCreateDTO;
import com.example.mallu.order.dto.OrderItemVO;
import com.example.mallu.order.dto.OrderVO;
import com.example.mallu.order.entity.Order;
import com.example.mallu.order.entity.OrderItem;
import com.example.mallu.order.mapper.OrderItemMapper;
import com.example.mallu.order.mapper.OrderMapper;
import com.example.mallu.product.entity.Product;
import com.example.mallu.product.mapper.ProductMapper;
import com.example.mallu.common.redis.SeckillRedisService;
import com.example.mallu.order.service.OrderAsyncService;
import com.example.mallu.seckill.entity.SeckillOrder;
import com.example.mallu.seckill.mapper.SeckillGoodsMapper;
import com.example.mallu.seckill.mapper.SeckillOrderMapper;
import com.example.mallu.user.entity.Address;
import com.example.mallu.user.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CartMapper cartMapper;
    private final AddressMapper addressMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponStrategyFactory couponStrategyFactory;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillRedisService seckillRedisService;
    private final OrderAsyncService orderAsyncService;

    private static final Integer DEFAULT_PAGE = 1;
    private static final Integer DEFAULT_SIZE = 10;

    @Transactional
    public OrderVO createOrder(OrderCreateDTO createDTO) {
        Long userId = UserContext.getUserId();

        Address address = addressMapper.selectById(createDTO.getAddressId(), userId);
        if (address == null) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        List<CartVO> cartItems = cartMapper.selectByUserId(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException(ResultCode.CART_EMPTY);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartVO cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            if (product == null || product.getStatus() == 0) {
                throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架: " + cartItem.getProductId());
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "商品库存不足: " + product.getName());
            }

            int affected = productMapper.decreaseStock(product.getId(), cartItem.getQuantity());
            if (affected == 0) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "商品库存不足: " + product.getName());
            }

            BigDecimal itemPrice = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemPrice);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(itemPrice);
            orderItems.add(orderItem);
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAddressId(address.getId());
        order.setTotalAmount(totalAmount);

        // 优惠券计算
        Long couponId = createDTO.getCouponId();
        if (couponId != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(couponId, userId);
            if (userCoupon == null) {
                throw new BusinessException(ResultCode.USER_COUPON_NOT_FOUND);
            }
            if (userCoupon.getStatus() != 1) {
                throw new BusinessException(ResultCode.USER_COUPON_NOT_AVAILABLE);
            }
            Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
            if (coupon == null || coupon.getStatus() == 0) {
                throw new BusinessException(ResultCode.COUPON_NOT_FOUND);
            }
            if (coupon.getStartTime().isAfter(LocalDateTime.now()) || coupon.getEndTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.COUPON_NOT_AVAILABLE, "优惠券已过期");
            }

            CouponStrategy strategy = couponStrategyFactory.getStrategy(coupon);
            BigDecimal discountAmount = strategy.calculateDiscount(totalAmount);
            order.setCouponId(couponId);
            order.setDiscountAmount(discountAmount);
            order.setPayAmount(totalAmount.subtract(discountAmount));

            userCouponMapper.updateUsed(userCoupon.getId(), order.getOrderNo());
        } else {
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setPayAmount(totalAmount);
        }

        order.setStatus(0);
        orderMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.batchInsert(orderItems);

        // 清空购物车，需要逐个删除。因为没有cartMapper.deleteByUserId方法
        for (CartVO cartItem : cartItems) {
            cartMapper.deleteById(cartItem.getId(), userId);
        }

        // 事务提交后再触发异步任务，确保虚拟线程能查到订单数据
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                orderAsyncService.afterOrderCreated(order.getId());
            }
        });

        return getOrderVO(order);
    }

    public OrderVO getOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId, UserContext.getUserId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return getOrderVO(order);
    }

    public OrderVO getOrderDetailByNo(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo, UserContext.getUserId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return getOrderVO(order);
    }

    public PageResult<OrderVO> listOrders(Integer page, Integer size) {
        page = page == null || page < 1 ? DEFAULT_PAGE : page;
        size = size == null || size < 1 ? DEFAULT_SIZE : size;

        Long userId = UserContext.getUserId();
        Integer offset = (page - 1) * size;
        List<Order> orders = orderMapper.selectByUserId(userId, offset, size);
        Long total = orderMapper.countByUserId(userId);

        List<OrderVO> vos = orders.stream()
                .map(this::getOrderVO)
                .toList();
        return PageResult.of(vos, total, page, size);
    }

    @Transactional
    public OrderVO payOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId, UserContext.getUserId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单不是待支付状态");
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateStatus(order);

        // 如果是秒杀订单，同步更新秒杀订单状态为已支付
        SeckillOrder seckillOrder = seckillOrderMapper.selectByOrderId(orderId);
        if (seckillOrder != null && seckillOrder.getUserId().equals(UserContext.getUserId())) {
            seckillOrder.setStatus(2);
            seckillOrderMapper.updateStatus(seckillOrder);
        }

        return getOrderVO(order);
    }

    @Transactional
    public OrderVO cancelOrder(Long orderId) {
        Long userId = UserContext.getUserId();
        Order order = orderMapper.selectById(orderId, userId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只有待支付订单可以取消");
        }
        order.setStatus(2);
        orderMapper.updateStatus(order);
        // 回滚普通商品库存
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }

        // 如果是秒杀订单，额外回滚秒杀库存和 Redis
        SeckillOrder seckillOrder = seckillOrderMapper.selectByOrderId(orderId);
        if (seckillOrder != null && seckillOrder.getUserId().equals(userId)) {
            seckillGoodsMapper.increaseStock(seckillOrder.getSeckillGoodsId(), 1);
            seckillRedisService.recoverStock(seckillOrder.getSeckillGoodsId(), userId);
            seckillOrder.setStatus(3);
            seckillOrderMapper.updateStatus(seckillOrder);
        }

        return getOrderVO(order);
    }

    @Transactional
    public OrderVO completeOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId, UserContext.getUserId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只有已支付订单可以完成");
        }
        order.setStatus(3);
        orderMapper.updateStatus(order);
        return getOrderVO(order);
    }

    private OrderVO getOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        vo.setItems(items.stream().map(this::toOrderItemVO).toList());
        return vo;
    }

    private OrderItemVO toOrderItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private String generateOrderNo() {
        return "O" + System.currentTimeMillis() + String.format("%06d", (int) (Math.random() * 1000000));
    }
}
