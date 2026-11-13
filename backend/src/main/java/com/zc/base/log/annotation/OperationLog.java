package com.zc.base.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在Controller方法上，自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 操作模块 */
    String module() default "";

    /** 操作描述 */
    String description() default "";

    /** 是否记录请求参数 */
    boolean saveParams() default true;

    /** 是否记录响应结果 */
    boolean saveResult() default true;

}
