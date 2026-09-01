package com.example.mallu.coupon.controller;

import com.example.mallu.common.result.Result;
import com.example.mallu.coupon.dto.CouponVO;
import com.example.mallu.coupon.dto.UserCouponVO;
import com.example.mallu.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public Result<List<CouponVO>> listAvailable() {
        return Result.success(couponService.listAvailableCoupons());
    }

    @PostMapping("/{id}/claim")
    public Result<UserCouponVO> claim(@PathVariable Long id) {
        return Result.success(couponService.claimCoupon(id));
    }

    @GetMapping("/my")
    public Result<List<UserCouponVO>> listMy() {
        return Result.success(couponService.listMyCoupons());
    }

    @GetMapping("/my/available")
    public Result<List<UserCouponVO>> listMyAvailable() {
        return Result.success(couponService.listMyAvailableCoupons());
    }
}
