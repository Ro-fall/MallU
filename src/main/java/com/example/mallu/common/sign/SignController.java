package com.example.mallu.common.sign;

import com.example.mallu.common.result.Result;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/sign")
public class SignController {

    private final RequestSignService requestSignService;

    public SignController(RequestSignService requestSignService) {
        this.requestSignService = requestSignService;
    }

    @PostMapping("/generate")
    public Result<SignGenerateResponse> generate(@RequestBody SignGenerateRequest request) {
        long timestamp = System.currentTimeMillis();
        String nonce = SignUtil.generateNonce();

        Map<String, String> params = new TreeMap<>(request.getParams() == null ? Map.of() : request.getParams());
        if (request.getBody() != null && !request.getBody().isBlank()) {
            params.put("body", request.getBody().trim());
        }
        String sign = requestSignService.generateSignature(params, timestamp, nonce);

        SignGenerateResponse response = new SignGenerateResponse();
        response.setTimestamp(timestamp);
        response.setNonce(nonce);
        response.setSign(sign);
        return Result.success(response);
    }

    @Data
    public static class SignGenerateRequest {
        private String method;
        private String path;
        private Map<String, String> params;
        private String body;
    }

    @Data
    public static class SignGenerateResponse {
        private Long timestamp;
        private String nonce;
        private String sign;
    }
}
