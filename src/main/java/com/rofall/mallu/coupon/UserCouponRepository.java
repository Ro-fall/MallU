package com.rofall.mallu.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);
    List<UserCoupon> findByUserIdOrderByIdDesc(Long userId);
}
