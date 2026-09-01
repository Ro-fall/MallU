package com.example.mallu.payment.controller;

import com.example.mallu.common.interceptor.SkipAuth;
import com.example.mallu.common.result.Result;
import com.example.mallu.payment.dto.PaymentCallbackDTO;
import com.example.mallu.payment.dto.PaymentCallbackVO;
import com.example.mallu.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/callback")
    @SkipAuth
    public Result<PaymentCallbackVO> callback(@RequestBody @Valid PaymentCallbackDTO dto) {
        return Result.success(paymentService.handleCallback(dto));
    }
}
