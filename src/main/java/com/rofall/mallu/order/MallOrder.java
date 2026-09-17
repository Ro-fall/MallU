package com.rofall.mallu.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mall_order")
@Getter
@NoArgsConstructor
public class MallOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_no", nullable = false, unique = true) private String orderNo;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "address_id", nullable = false) private Long addressId;
    @Column(name = "user_coupon_id") private Long userCouponId;
    @Column(name = "total_amount", nullable = false) private BigDecimal totalAmount;
    @Column(name = "discount_amount", nullable = false) private BigDecimal discountAmount;
    @Column(name = "pay_amount", nullable = false) private BigDecimal payAmount;
    @Column(nullable = false) private String status;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public MallOrder(String orderNo, Long userId, Long addressId, Long userCouponId, BigDecimal totalAmount,
                     BigDecimal discountAmount, BigDecimal payAmount) {
        this.orderNo = orderNo; this.userId = userId; this.addressId = addressId; this.userCouponId = userCouponId;
        this.totalAmount = totalAmount; this.discountAmount = discountAmount; this.payAmount = payAmount;
        this.status = "PENDING_PAYMENT"; this.createdAt = LocalDateTime.now(); this.expiresAt = createdAt.plusMinutes(30);
    }
    public boolean pendingPayment() { return "PENDING_PAYMENT".equals(status); }
    public void cancel() { if (!pendingPayment()) throw new IllegalStateException("订单当前不可取消"); status = "CANCELLED"; }
    public void pay() { if (!pendingPayment()) throw new IllegalStateException("订单当前不可支付"); status = "PAID"; paidAt = LocalDateTime.now(); }
}
