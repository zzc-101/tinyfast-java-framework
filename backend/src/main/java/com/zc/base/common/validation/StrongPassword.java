package com.zc.base.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 密码强度：至少 8 位，同时包含字母和数字。
 * null 和空白视为通过，是否必填由 @NotBlank 决定。
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default "密码至少 8 位，且必须同时包含字母和数字";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
