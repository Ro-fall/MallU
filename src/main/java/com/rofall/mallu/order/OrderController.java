package com.rofall.mallu.order;

import com.rofall.mallu.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    @PostMapping public ApiResponse<OrderResponse> create(@RequestBody @Valid OrderCreateRequest request) { return ApiResponse.success(orderService.create(request)); }
    @GetMapping public ApiResponse<List<OrderResponse>> list() { return ApiResponse.success(orderService.list()); }
    @GetMapping("/{orderId}") public ApiResponse<OrderResponse> detail(@PathVariable Long orderId) { return ApiResponse.success(orderService.detail(orderId)); }
    @PostMapping("/{orderId}/cancel") public ApiResponse<OrderResponse> cancel(@PathVariable Long orderId) { return ApiResponse.success(orderService.cancel(orderId)); }
    @PostMapping("/{orderId}/pay") public ApiResponse<OrderResponse> pay(@PathVariable Long orderId) { return ApiResponse.success(orderService.pay(orderId)); }
}
