package com.example.mallu.testsupport;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.interceptor.SkipAuth;
import com.example.mallu.common.result.Result;
import com.example.mallu.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.Map;

@RestController
@RequestMapping("/api/test-support/sessions")
@RequiredArgsConstructor
@SkipAuth
public class TestSupportController {

    private final TestSupportService testSupportService;

    @PostMapping
    public Result<Map<String, Object>> create(HttpServletRequest request) {
        requireLoopback(request);
        return Result.success(testSupportService.createSession());
    }

    @DeleteMapping("/{userId}")
    public Result<Void> cleanup(@PathVariable Long userId, @RequestParam String username, HttpServletRequest request) {
        requireLoopback(request);
        testSupportService.cleanupSession(userId, username);
        return Result.success();
    }

    private void requireLoopback(HttpServletRequest request) {
        try {
            if (!InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
                throw new BusinessException(ResultCode.FORBIDDEN, "测试数据接口仅允许本机访问");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无法确认请求来源");
        }
    }
}