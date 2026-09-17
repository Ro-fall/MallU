package com.rofall.mallu.reliability;

import com.rofall.mallu.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderTokenController {
    private final OrderTokenService orderTokenService;
    @PostMapping("/tokens")
    public ApiResponse<OrderTokenResponse> create(@RequestBody @Valid OrderTokenRequest request) { return ApiResponse.success(orderTokenService.create(request)); }
}
