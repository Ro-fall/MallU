package com.example.mallu.coupon.mapper;

import com.example.mallu.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponMapper {

    Coupon selectById(Long id);

    List<Coupon> selectAvailable();

    int decreaseRemainingCount(@Param("id") Long id, @Param("quantity") Integer quantity);
}
