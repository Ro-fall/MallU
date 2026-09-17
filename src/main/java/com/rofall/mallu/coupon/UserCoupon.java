package com.rofall.mallu.coupon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_coupon")
@Getter
@NoArgsConstructor
public class UserCoupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "coupon_id", nullable = false) private Long couponId;
    @Column(nullable = false) private String status;
    @Column(name = "used_order_no") private String usedOrderNo;

    public UserCoupon(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = "AVAILABLE";
    }

    public void use(String orderNo) {
        if (!"AVAILABLE".equals(status)) throw new IllegalStateException("优惠券不可用");
        status = "USED";
        usedOrderNo = orderNo;
    }

    public void release() {
        if ("USED".equals(status)) {
            status = "AVAILABLE";
            usedOrderNo = null;
        }
    }
}
