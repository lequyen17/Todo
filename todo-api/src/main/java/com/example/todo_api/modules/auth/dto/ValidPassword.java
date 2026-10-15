package com.example.todo_api.modules.auth.dto;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Quy tắc mật khẩu mới (SRS): 8-72 ký tự, có cả chữ và số.
 * 72 = giới hạn byte của BCrypt. Dùng chung cho đăng ký, đổi mật khẩu, đặt lại mật khẩu.
 */
@NotBlank
@Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "Password must contain both letters and numbers")
@Constraint(validatedBy = {})
@Documented
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface ValidPassword {

    String message() default "Invalid password";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
