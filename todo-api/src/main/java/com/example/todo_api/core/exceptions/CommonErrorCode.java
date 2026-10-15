package com.example.todo_api.core.exceptions;

import org.springframework.http.HttpStatus;

/** Lỗi chung, không thuộc nghiệp vụ module nào. */
public enum CommonErrorCode implements ErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid request data"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request body"),
    CONFLICT(HttpStatus.CONFLICT, "The request conflicts with existing data, please try again"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, please try again in a few minutes"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong, please try again later");

    private final HttpStatus status;
    private final String message;

    CommonErrorCode(HttpStatus status, String message) {
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
