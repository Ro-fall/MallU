package com.example.mallu.order.controller;

import com.example.mallu.common.result.Result;
import com.example.mallu.order.entity.OrderEventLog;
import com.example.mallu.order.mapper.OrderEventLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderEventLogController {

    private final OrderEventLogMapper orderEventLogMapper;

    @GetMapping("/{orderId}/events")
    public Result<List<OrderEventLog>> listEvents(@PathVariable Long orderId) {
        return Result.success(orderEventLogMapper.selectByOrderId(orderId));
    }
}
