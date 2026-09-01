package com.example.mallu.common.sign;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class SignUtil {

    public static final String SIGN_HEADER = "X-Sign";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";
    public static final String NONCE_HEADER = "X-Nonce";

    public static String generateSignature(Map<String, String> params, long timestamp, String nonce, String secret) {
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\u0026");
        }
        sb.append("timestamp=").append(timestamp).append("\u0026");
        sb.append("nonce=").append(nonce);
        return hmacSha256Hex(sb.toString(), secret);
    }

    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
}
