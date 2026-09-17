package com.rofall.mallu.coupon;

import com.rofall.mallu.common.BusinessException;
import com.rofall.mallu.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public CouponResponse claim(Long couponId) {
        Long userId = UserContext.requireUserId();
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new BusinessException(4091, HttpStatus.CONFLICT, "该优惠券已领取");
        }
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(4045, HttpStatus.NOT_FOUND, "优惠券不存在"));
        if (!coupon.canClaim(LocalDateTime.now())) {
            throw new BusinessException(4092, HttpStatus.CONFLICT, "优惠券当前不可领取");
        }
        coupon.decreaseRemaining();
        return toResponse(userCouponRepository.save(new UserCoupon(userId, couponId)), coupon);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> mine() {
        return userCouponRepository.findByUserIdOrderByIdDesc(UserContext.requireUserId()).stream()
                .map(userCoupon -> toResponse(userCoupon, couponRepository.findById(userCoupon.getCouponId())
                        .orElseThrow(() -> new BusinessException(4045, HttpStatus.NOT_FOUND, "优惠券不存在"))))
                .toList();
    }

    private CouponResponse toResponse(UserCoupon userCoupon, Coupon coupon) {
        return new CouponResponse(userCoupon.getId(), coupon.getId(), coupon.getName(), coupon.getType(),
                coupon.getThresholdAmount(), coupon.getValue(), userCoupon.getStatus(), coupon.getEndTime());
    }
}
