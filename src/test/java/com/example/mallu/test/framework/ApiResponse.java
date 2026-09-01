package com.example.mallu.test.framework;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 统一响应解析：解包 MallU Result<T> 结构 {code, message, data}
 */
public class ApiResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JsonNode root;

    public ApiResponse(String json) {
        try {
            this.root = OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("响应不是合法 JSON: " + json, e);
        }
    }

    public int code() {
        return root.path("code").asInt(-1);
    }

    public String message() {
        return root.path("message").asText();
    }

    public boolean success() {
        return code() == 200;
    }

    public JsonNode data() {
        return root.path("data");
    }

    public String dataAsString() {
        return data().isNull() ? null : data().asText();
    }

    public JsonNode field(String name) {
        return data().path(name);
    }

    public <T> T dataTo(Class<T> clazz) {
        try {
            return OBJECT_MAPPER.treeToValue(data(), clazz);
        } catch (Exception e) {
            throw new IllegalStateException("data 反序列化失败", e);
        }
    }

    public Map<String, Object> dataAsMap() {
        try {
            return OBJECT_MAPPER.convertValue(data(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("data 转 Map 失败", e);
        }
    }

    @Override
    public String toString() {
        return root == null ? "null" : root.toString();
    }
}
