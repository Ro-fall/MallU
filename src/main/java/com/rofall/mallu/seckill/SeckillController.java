package com.rofall.mallu.seckill;

import com.rofall.mallu.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {
    private final SeckillService seckillService;
    @GetMapping("/activities/{activityId}/goods") public ApiResponse<List<SeckillGoodsResponse>> list(@PathVariable Long activityId) { return ApiResponse.success(seckillService.list(activityId)); }
    @PostMapping("/goods/{goodsId}/orders") public ApiResponse<SeckillResultResponse> submit(@PathVariable Long goodsId, @RequestBody @Valid SeckillRequest request) { return ApiResponse.success(seckillService.submit(goodsId, request)); }
    @GetMapping("/goods/{goodsId}/result") public ApiResponse<SeckillResultResponse> result(@PathVariable Long goodsId) { return ApiResponse.success(seckillService.result(goodsId)); }
}
