package com.example.mallu.order.controller;

import com.example.mallu.common.idempotent.Idempotent;
import com.example.mallu.common.result.PageResult;
import com.example.mallu.common.result.Result;
import com.example.mallu.common.sign.ApiSign;
import com.example.mallu.order.dto.OrderCreateDTO;
import com.example.mallu.order.dto.OrderVO;
import com.example.mallu.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Idempotent
    @ApiSign
    public Result<OrderVO> create(@RequestBody @Valid OrderCreateDTO createDTO) {
        return Result.success(orderService.createOrder(createDTO));
    }

    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(orderService.listOrders(page, size));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @PutMapping("/{id}/pay")
    @Idempotent
    @ApiSign
    public Result<OrderVO> pay(@PathVariable Long id) {
        return Result.success(orderService.payOrder(id));
    }

    @PutMapping("/{id}/cancel")
    public Result<OrderVO> cancel(@PathVariable Long id) {
        return Result.success(orderService.cancelOrder(id));
    }

    @PutMapping("/{id}/complete")
    public Result<OrderVO> complete(@PathVariable Long id) {
        return Result.success(orderService.completeOrder(id));
    }
}
