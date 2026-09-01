package com.example.mallu.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateDTO {

    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    private Long couponId;
}
