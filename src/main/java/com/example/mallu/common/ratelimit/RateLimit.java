package com.example.mallu.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求数
     */
    int limit() default 1;

    /**
     * 时间窗口，单位秒
     */
    int windowSeconds() default 1;

    /**
     * 限流范围：用户级或全局
     */
    Scope scope() default Scope.USER;

    enum Scope {
        USER,
        GLOBAL
    }
}
