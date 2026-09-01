package com.example.mallu.test.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一断言模块：校验业务码、data 字段、错误信息。
 */
public final class TestAssertions {

    private TestAssertions() {
    }

    public static void assertSuccess(ApiResponse resp) {
        assertEquals(200, resp.code(), "期望业务码 200，实际: " + resp.code() + " message=" + resp.message());
    }

    public static void assertCode(ApiResponse resp, int expectedCode) {
        assertEquals(expectedCode, resp.code(),
                "期望业务码 " + expectedCode + "，实际: " + resp.code() + " message=" + resp.message());
    }

    public static void assertMessageContains(ApiResponse resp, String keyword) {
        assertNotNull(resp.message(), "message 不应为空");
        assertTrue(resp.message().contains(keyword),
                "期望 message 包含 [" + keyword + "]，实际: " + resp.message());
    }

    public static void assertDataField(ApiResponse resp, String field) {
        assertNotNull(resp.field(field), "data 应包含字段: " + field + "，实际 data=" + resp.data());
    }

    public static void assertDataFieldEquals(ApiResponse resp, String field, Object expected) {
        assertDataField(resp, field);
        assertEquals(String.valueOf(expected), resp.field(field).asText(),
                "字段 " + field + " 期望 " + expected + "，实际 " + resp.field(field));
    }
}
