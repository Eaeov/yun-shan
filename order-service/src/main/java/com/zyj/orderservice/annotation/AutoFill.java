package com.zyj.orderservice.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author：zyj
 * @Package：com.zyj.orderservice.annotation
 * @Project：yun-shan
 * @name：AutoFill
 * @Date：06 12月 2025  14:06
 * @Filename：AutoFill
 */

/*
 * 自定义注解: 标识需要自动填充公共字段的mapper方法
 *
 */
@Target(ElementType.METHOD)  // 注解只能在方法上使用
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时生效
public @interface AutoFill {
    // 注解属性。指定操作类型
    OperationType value();
}
