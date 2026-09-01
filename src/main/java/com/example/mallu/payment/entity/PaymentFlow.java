package com.example.mallu.payment.entity;

import lombok.Data;

import java.math.BigDecimal;

/** 支付平台回调流水；tradeNo 在数据库层唯一，重启后仍保持幂等。 */
@Data
public class PaymentFlow {
    private Long id;
    private String tradeNo;
    private String orderNo;
    private Long orderId;
    private BigDecimal payAmount;
    private String result;
}
