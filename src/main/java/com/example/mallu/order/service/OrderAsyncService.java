package com.example.mallu.order.service;

import com.example.mallu.order.entity.Order;
import com.example.mallu.order.entity.OrderEventLog;
import com.example.mallu.order.entity.OrderItem;
import com.example.mallu.order.mapper.OrderEventLogMapper;
import com.example.mallu.order.mapper.OrderItemMapper;
import com.example.mallu.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAsyncService {

    private final ExecutorService virtualThreadExecutor;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderEventLogMapper orderEventLogMapper;

    public void afterOrderCreated(Long orderId) {
        CompletableFuture.runAsync(() -> sendCreateNotification(orderId), virtualThreadExecutor);
        CompletableFuture.runAsync(() -> recordOrderBuryPoint(orderId), virtualThreadExecutor);
        CompletableFuture.runAsync(() -> updateProductSales(orderId), virtualThreadExecutor);
    }

    private void sendCreateNotification(Long orderId) {
        try {
            Thread.sleep(50); // 模拟通知发送耗时
            saveLog(orderId, "NOTIFY", "订单创建通知已发送", 1);
            log.info("[虚拟线程] 订单 {} 创建通知发送成功", orderId);
        } catch (Exception e) {
            log.error("[虚拟线程] 订单 {} 创建通知发送失败", orderId, e);
            saveLog(orderId, "NOTIFY", "订单创建通知发送失败: " + e.getMessage(), 0);
        }
    }

    private void recordOrderBuryPoint(Long orderId) {
        try {
            Order order = orderMapper.selectByIdForSystem(orderId);
            if (order == null) {
                return;
            }
            saveLog(orderId, "BURY", "用户 " + order.getUserId() + " 创建订单，金额: " + order.getPayAmount(), 1);
            log.info("[虚拟线程] 订单 {} 埋点记录成功", orderId);
        } catch (Exception e) {
            log.error("[虚拟线程] 订单 {} 埋点记录失败", orderId, e);
            saveLog(orderId, "BURY", "埋点记录失败: " + e.getMessage(), 0);
        }
    }

    private void updateProductSales(Long orderId) {
        try {
            List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
            StringBuilder sb = new StringBuilder();
            for (OrderItem item : items) {
                sb.append(item.getProductName()).append(" x ").append(item.getQuantity()).append("; ");
            }
            saveLog(orderId, "STAT", "销量统计更新: " + sb, 1);
            log.info("[虚拟线程] 订单 {} 销量统计更新成功", orderId);
        } catch (Exception e) {
            log.error("[虚拟线程] 订单 {} 销量统计更新失败", orderId, e);
            saveLog(orderId, "STAT", "销量统计更新失败: " + e.getMessage(), 0);
        }
    }

    private void saveLog(Long orderId, String eventType, String content, Integer status) {
        OrderEventLog log = new OrderEventLog();
        log.setOrderId(orderId);
        log.setEventType(eventType);
        log.setContent(content);
        log.setStatus(status);
        orderEventLogMapper.insert(log);
    }
}
