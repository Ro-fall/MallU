package com.rofall.mallu.order;

import com.rofall.mallu.address.AddressRepository;
import com.rofall.mallu.cart.CartItem;
import com.rofall.mallu.cart.CartItemRepository;
import com.rofall.mallu.catalog.Product;
import com.rofall.mallu.catalog.ProductRepository;
import com.rofall.mallu.common.BusinessException;
import com.rofall.mallu.coupon.Coupon;
import com.rofall.mallu.coupon.CouponRepository;
import com.rofall.mallu.coupon.UserCoupon;
import com.rofall.mallu.coupon.UserCouponRepository;
import com.rofall.mallu.security.UserContext;
import com.rofall.mallu.user.MallUser;
import com.rofall.mallu.user.MallUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final MallOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final MallUserRepository userRepository;

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        Long userId = UserContext.requireUserId();
        if (new HashSet<>(request.cartItemIds()).size() != request.cartItemIds().size()) {
            throw new BusinessException(4004, HttpStatus.BAD_REQUEST, "购物车项不能重复");
        }
        addressRepository.findByIdAndUserId(request.addressId(), userId)
                .orElseThrow(() -> new BusinessException(4043, HttpStatus.NOT_FOUND, "收货地址不存在"));
        List<CartItem> cartItems = cartItemRepository.findByIdInAndUserId(request.cartItemIds(), userId);
        if (cartItems.size() != request.cartItemIds().size()) {
            throw new BusinessException(4044, HttpStatus.NOT_FOUND, "购物车项不存在或不属于当前用户");
        }
        Map<Long, Product> products = new HashMap<>();
        for (Product product : productRepository.findAllById(cartItems.stream().map(CartItem::getProductId).toList())) products.put(product.getId(), product);
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = products.get(item.getProductId());
            if (product == null || !Boolean.TRUE.equals(product.getStatus())) throw new BusinessException(4042, HttpStatus.NOT_FOUND, "商品不存在或已下架");
            if (product.getStock() < item.getQuantity()) throw new BusinessException(4003, HttpStatus.CONFLICT, "商品库存不足");
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        Coupon coupon = null; UserCoupon userCoupon = null;
        if (request.userCouponId() != null) {
            userCoupon = userCouponRepository.findByIdAndUserId(request.userCouponId(), userId)
                    .orElseThrow(() -> new BusinessException(4046, HttpStatus.NOT_FOUND, "用户优惠券不存在"));
            if (!"AVAILABLE".equals(userCoupon.getStatus())) throw new BusinessException(4093, HttpStatus.CONFLICT, "优惠券已被使用");
            coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new BusinessException(4045, HttpStatus.NOT_FOUND, "优惠券不存在"));
            if (!coupon.validNow(LocalDateTime.now())) throw new BusinessException(4092, HttpStatus.CONFLICT, "优惠券当前不可用");
        }
        BigDecimal discount = OrderAmountCalculator.discount(total, coupon);
        BigDecimal pay = total.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        String orderNo = "MO" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MallOrder order = orderRepository.save(new MallOrder(orderNo, userId, request.addressId(), request.userCouponId(), total, discount, pay));
        if (userCoupon != null) userCoupon.use(orderNo);
        List<OrderItem> items = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = products.get(cartItem.getProductId());
            product.decreaseStock(cartItem.getQuantity());
            items.add(new OrderItem(order.getId(), product.getId(), product.getName(), product.getPrice(), cartItem.getQuantity()));
        }
        orderItemRepository.saveAll(items);
        cartItemRepository.deleteAll(cartItems);
        return response(order, items);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() { return orderRepository.findByUserIdOrderByCreatedAtDesc(UserContext.requireUserId()).stream().map(this::response).toList(); }

    @Transactional(readOnly = true)
    public OrderResponse detail(Long orderId) { return response(owned(orderId)); }

    @Transactional
    public OrderResponse cancel(Long orderId) {
        MallOrder order = owned(orderId);
        if (!order.pendingPayment()) throw new BusinessException(4094, HttpStatus.CONFLICT, "只有待支付订单可以取消");
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        for (OrderItem item : items) productRepository.findById(item.getProductId()).ifPresent(product -> product.increaseStock(item.getQuantity()));
        if (order.getUserCouponId() != null) userCouponRepository.findById(order.getUserCouponId()).ifPresent(UserCoupon::release);
        order.cancel();
        return response(order, items);
    }

    @Transactional
    public OrderResponse pay(Long orderId) {
        MallOrder order = owned(orderId);
        if (!order.pendingPayment()) throw new BusinessException(4095, HttpStatus.CONFLICT, "订单当前不可支付");
        order.pay();
        MallUser user = userRepository.findById(order.getUserId()).orElseThrow(() -> new BusinessException(4041, HttpStatus.NOT_FOUND, "用户不存在"));
        user.addPoints(order.getPayAmount().setScale(0, RoundingMode.DOWN).intValue());
        return response(order);
    }

    private MallOrder owned(Long orderId) { return orderRepository.findByIdAndUserId(orderId, UserContext.requireUserId()).orElseThrow(() -> new BusinessException(4047, HttpStatus.NOT_FOUND, "订单不存在")); }
    private OrderResponse response(MallOrder order) { return response(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId())); }
    private OrderResponse response(MallOrder order, List<OrderItem> items) {
        int points = "PAID".equals(order.getStatus()) ? order.getPayAmount().setScale(0, RoundingMode.DOWN).intValue() : 0;
        return new OrderResponse(order.getId(), order.getOrderNo(), order.getStatus(), order.getTotalAmount(), order.getDiscountAmount(), order.getPayAmount(), points,
                order.getExpiresAt(), order.getPaidAt(), items.stream().map(item -> new OrderItemResponse(item.getProductId(), item.getProductName(), item.getProductPrice(), item.getQuantity(), item.getTotalAmount())).toList());
    }
}
