package com.qiujie.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 速率限制注解
 * 用于控制接口访问频率
 *
 * @author qiujie
 * @date 2026-06-09
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求次数
     */
    int value() default 10;

    /**
     * 时间窗口大小(秒)
     */
    int timeout() default 60;

    /**
     * 限制类型: IP, USER, GLOBAL
     */
    LimitType type() default LimitType.USER;

    enum LimitType {
        /**
         * 按 IP 地址限制
         */
        IP,
        /**
         * 按用户 ID 限制
         */
        USER,
        /**
         * 全局限制
         */
        GLOBAL
    }
}
