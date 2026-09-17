package com.rofall.mallu.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{4,32}", message = "用户名格式不合法") String username,
        @NotBlank @Size(min = 8, max = 72, message = "密码长度不合法") String password
) {
}
