package com.qiujie.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author qiujie
 * @Date 2022/3/29
 * @Version 1.0
 * @deprecated 请使用 EasyExcel 的 {@code @ExcelProperty} 注解替代
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelColumn {

    /**
     * 列的名称
     *
     * @return
     */
    String value() default "";


}
