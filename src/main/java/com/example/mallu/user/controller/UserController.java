package com.example.mallu.user.controller;

import com.example.mallu.common.interceptor.SkipAuth;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.result.Result;
import com.example.mallu.user.dto.*;
import com.example.mallu.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @SkipAuth
    public Result<LoginVO> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        return Result.success(userService.register(registerDTO));
    }

    @PostMapping("/login")
    @SkipAuth
    public Result<LoginVO> login(@RequestBody @Valid UserLoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        return Result.success(userService.getUserById(UserContext.getUserId()));
    }
}
