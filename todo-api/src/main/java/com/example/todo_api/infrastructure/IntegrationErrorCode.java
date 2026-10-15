package com.example.todo_api.infrastructure;

import org.springframework.http.HttpStatus;

import com.example.todo_api.core.exceptions.ErrorCode;

/** Lỗi từ các dịch vụ bên ngoài (Google, R2). */
public enum IntegrationErrorCode implements ErrorCode {

    GOOGLE_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not enabled"),
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, "Google sign-in failed, please try again"),
    AVATAR_UPLOAD_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "Avatar upload is not configured");

    private final HttpStatus status;
    private final String message;

    IntegrationErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
