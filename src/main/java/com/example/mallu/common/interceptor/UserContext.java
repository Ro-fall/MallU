package com.example.mallu.common.interceptor;

public class UserContext {

    private static final ThreadLocal<LoginUser> USER_HOLDER = new ThreadLocal<>();

    public static void setUser(LoginUser user) {
        USER_HOLDER.set(user);
    }

    public static LoginUser getUser() {
        return USER_HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = USER_HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
