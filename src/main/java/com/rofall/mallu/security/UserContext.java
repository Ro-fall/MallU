package com.rofall.mallu.security;

public final class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long requireUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("当前请求未绑定用户");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
