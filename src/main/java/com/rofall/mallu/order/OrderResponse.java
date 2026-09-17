package com.rofall.mallu.order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record OrderResponse(Long id, String orderNo, String status, BigDecimal totalAmount, BigDecimal discountAmount,
                            BigDecimal payAmount, int pointsAwarded, LocalDateTime expiresAt, LocalDateTime paidAt,
                            List<OrderItemResponse> items) { }
