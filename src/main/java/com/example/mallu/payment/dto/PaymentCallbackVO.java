package com.example.mallu.payment.dto;

import lombok.Data;

@Data
public class PaymentCallbackVO {

    private String orderNo;
    private String tradeNo;
    private Boolean success;
    private Integer orderStatus;
    private String message;
}
