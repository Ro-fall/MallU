package com.rofall.mallu.user;

public record AuthResponse(Long userId, String username, String token, int points) {
}
