package com.example.mallu.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long addressId;
    private Long couponId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime createdAt;
    private List<OrderItemVO> items;
}
