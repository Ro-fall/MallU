package com.example.mallu.seckill.controller;

import com.example.mallu.common.ratelimit.RateLimit;
import com.example.mallu.common.result.Result;
import com.example.mallu.common.sign.ApiSign;
import com.example.mallu.seckill.dto.SeckillGoodsVO;
import com.example.mallu.seckill.dto.SeckillOrderVO;
import com.example.mallu.seckill.entity.SeckillActivity;
import com.example.mallu.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @GetMapping("/activities")
    public Result<List<SeckillActivity>> listActivities() {
        return Result.success(seckillService.listActivities());
    }

    @GetMapping("/activities/{activityId}/goods")
    public Result<List<SeckillGoodsVO>> listGoods(@PathVariable Long activityId) {
        return Result.success(seckillService.listGoods(activityId));
    }

    @GetMapping("/goods/{seckillGoodsId}")
    public Result<SeckillGoodsVO> goodsDetail(@PathVariable Long seckillGoodsId) {
        return Result.success(seckillService.goodsDetail(seckillGoodsId));
    }

    @PostMapping("/goods/{seckillGoodsId}/seckill")
    @RateLimit(limit = 1, windowSeconds = 3, scope = RateLimit.Scope.USER)
    @ApiSign
    public Result<SeckillOrderVO> seckill(@PathVariable Long seckillGoodsId,
                                           @RequestParam Long addressId) {
        return Result.success(seckillService.seckill(seckillGoodsId, addressId));
    }

    @GetMapping("/orders/{seckillOrderId}")
    public Result<SeckillOrderVO> getResult(@PathVariable Long seckillOrderId) {
        return Result.success(seckillService.getResult(seckillOrderId));
    }

    @GetMapping("/result/{seckillGoodsId}")
    public Result<SeckillOrderVO> getResultByGoodsId(@PathVariable Long seckillGoodsId) {
        return Result.success(seckillService.getResultByGoodsId(seckillGoodsId));
    }
}
