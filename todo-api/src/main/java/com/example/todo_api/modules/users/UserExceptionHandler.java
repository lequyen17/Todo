package com.example.todo_api.modules.users;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.example.todo_api.core.exceptions.ApiProblem;

/**
 * Upload duy nhất của app là avatar, nên file vượt giới hạn multipart được báo bằng mã lỗi của avatar.
 * Ưu tiên cao hơn GlobalExceptionHandler (vốn cũng xử lý lỗi này, trả 413 chung chung).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Object> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ApiProblem.response(UserErrorCode.AVATAR_TOO_LARGE);
    }
}
