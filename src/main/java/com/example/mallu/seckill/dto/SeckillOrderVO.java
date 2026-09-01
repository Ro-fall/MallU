package com.example.mallu.seckill.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillOrderVO {

    private Long id;
    private Long seckillGoodsId;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Integer status;
    private BigDecimal payAmount;
}
