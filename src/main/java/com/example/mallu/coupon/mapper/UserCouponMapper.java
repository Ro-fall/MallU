package com.example.mallu.coupon.mapper;

import com.example.mallu.coupon.dto.UserCouponVO;
import com.example.mallu.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCouponMapper {

    int insert(UserCoupon userCoupon);

    int updateUsed(@Param("id") Long id, @Param("orderNo") String orderNo);

    int restoreAvailable(@Param("id") Long id, @Param("userId") Long userId, @Param("orderNo") String orderNo);

    UserCoupon selectByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    UserCoupon selectById(@Param("id") Long id, @Param("userId") Long userId);

    List<UserCouponVO> selectByUserId(Long userId);

    List<UserCouponVO> selectAvailableByUserId(Long userId);
}
