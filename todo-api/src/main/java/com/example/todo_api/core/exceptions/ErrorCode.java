package com.example.todo_api.core.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi trả về cho client: {"code": name(), "message": ...}.
 * Mỗi module khai báo enum riêng (CommonErrorCode, AuthErrorCode, UserErrorCode...) để core không phụ thuộc module.
 * message() có thể chứa %s/%d, điền bằng tham số của ApiException.
 */
public interface ErrorCode {

    String name();

    HttpStatus status();

    String message();
}
