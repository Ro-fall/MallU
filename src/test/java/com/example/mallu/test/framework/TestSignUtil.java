package com.example.mallu.test.framework;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import java.util.Map;

/**
 * 签名工具：与后端 SignUtil 算法保持一致。
 * 参数排序拼接 + timestamp + nonce，HMAC-SHA256，hex 输出。
 */
public class TestSignUtil {

    public static final String SIGN_HEADER = "X-Sign";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";
    public static final String NONCE_HEADER = "X-Nonce";

    public static String generateSignature(Map<String, String> params, long timestamp, String nonce) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\u0026");
        }
        sb.append("timestamp=").append(timestamp).append("\u0026");
        sb.append("nonce=").append(nonce);
        return hmacSha256Hex(sb.toString(), getSecret());
    }

    public static String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    private static String getSecret() {
        String secret = System.getProperty("mallu.signature.secret", System.getenv("MALLU_SIGNATURE_SECRET"));
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("请通过 -Dmallu.signature.secret 或 MALLU_SIGNATURE_SECRET 配置测试签名密钥");
        }
        return secret;
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
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
