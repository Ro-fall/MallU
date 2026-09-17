package com.rofall.mallu.coupon;

import com.rofall.mallu.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @PostMapping("/{couponId}/claim")
    public ApiResponse<CouponResponse> claim(@PathVariable Long couponId) { return ApiResponse.success(couponService.claim(couponId)); }

    @GetMapping("/mine")
    public ApiResponse<List<CouponResponse>> mine() { return ApiResponse.success(couponService.mine()); }
}
