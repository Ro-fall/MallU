package com.example.mallu.coupon.service;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.coupon.dto.CouponVO;
import com.example.mallu.coupon.dto.UserCouponVO;
import com.example.mallu.coupon.entity.Coupon;
import com.example.mallu.coupon.entity.UserCoupon;
import com.example.mallu.coupon.mapper.CouponMapper;
import com.example.mallu.coupon.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public List<CouponVO> listAvailableCoupons() {
        return couponMapper.selectAvailable().stream()
                .map(this::toCouponVO)
                .toList();
    }

    @Transactional
    public UserCouponVO claimCoupon(Long couponId) {
        Long userId = UserContext.getUserId();

        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        if (coupon.getRemainingCount() <= 0) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券已领完");
        }
        if (coupon.getStartTime().isAfter(LocalDateTime.now()) || coupon.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券不在有效期内");
        }

        if (userCouponMapper.selectByUserIdAndCouponId(userId, couponId) != null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "已经领取过该优惠券");
        }

        int affected = couponMapper.decreaseRemainingCount(couponId, 1);
        if (affected == 0) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券已领完");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(1);
        userCouponMapper.insert(userCoupon);

        return toUserCouponVO(userCoupon, coupon);
    }

    public List<UserCouponVO> listMyCoupons() {
        return userCouponMapper.selectByUserId(UserContext.getUserId());
    }

    public List<UserCouponVO> listMyAvailableCoupons() {
        return userCouponMapper.selectAvailableByUserId(UserContext.getUserId());
    }

    private CouponVO toCouponVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        BeanUtils.copyProperties(coupon, vo);
        return vo;
    }

    private UserCouponVO toUserCouponVO(UserCoupon userCoupon, Coupon coupon) {
        UserCouponVO vo = new UserCouponVO();
        BeanUtils.copyProperties(userCoupon, vo);
        if (coupon != null) {
            vo.setCouponName(coupon.getName());
            vo.setCouponType(coupon.getType());
        }
        return vo;
    }
}
