package com.example.mallu.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCallbackDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal payAmount;

    @NotBlank(message = "支付流水号不能为空")
    private String tradeNo;

    @NotBlank(message = "支付结果不能为空")
    private String result;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(result);
    }
}
