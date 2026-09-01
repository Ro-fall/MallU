package com.example.mallu.common.sign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RequestSignService {
    private final String secret;

    public RequestSignService(@Value("${signature.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("MALLU_SIGNATURE_SECRET 未配置");
        }
        this.secret = secret;
    }

    public String generateSignature(Map<String, String> params, long timestamp, String nonce) {
        return SignUtil.generateSignature(params, timestamp, nonce, secret);
    }
}
