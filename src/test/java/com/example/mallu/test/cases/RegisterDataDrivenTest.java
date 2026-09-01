package com.example.mallu.test.cases;

import com.example.mallu.test.framework.ApiClient;
import com.example.mallu.test.framework.ApiResponse;
import com.example.mallu.test.framework.TestAssertions;
import com.example.mallu.test.framework.TestDataProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

/**
 * 数据驱动测试：注册接口正常/异常/边界用例，测试数据与脚本分离。
 */
public class RegisterDataDrivenTest {

    private ApiClient client;

    @BeforeEach
    void setUp() {
        client = new ApiClient(System.getProperty("mallu.base.url", "http://localhost:8080"));
    }

    static Stream<JsonNode> registerCases() {
        return TestDataProvider.loadCases("testdata/register_cases.json").stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("registerCases")
    @DisplayName("注册接口-数据驱动")
    void register(JsonNode caseNode) {
        String username = caseNode.path("username").asText() + "_" + System.currentTimeMillis() % 100000;
        String password = caseNode.path("password").asText();
        String phone = caseNode.path("phone").isNull() ? null : caseNode.path("phone").asText();
        String email = caseNode.path("email").isNull() ? null : caseNode.path("email").asText();
        int expectedCode = caseNode.path("expectedCode").asInt();

        ApiResponse resp = client.register(username, password, phone, email);
        TestAssertions.assertCode(resp, expectedCode);
    }
}
