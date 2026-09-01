package com.example.mallu.test.framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试数据提供器：从 testdata/*.json 读取数据驱动用例，实现测试数据与脚本分离。
 */
public class TestDataProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<JsonNode> loadCases(String resourcePath) {
        try (InputStream in = TestDataProvider.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("测试数据文件不存在: " + resourcePath);
            }
            JsonNode root = OBJECT_MAPPER.readTree(in);
            JsonNode cases = root.isArray() ? root : root.path("cases");
            List<JsonNode> list = new ArrayList<>();
            cases.forEach(list::add);
            return list;
        } catch (Exception e) {
            throw new IllegalStateException("加载测试数据失败: " + resourcePath, e);
        }
    }
}
