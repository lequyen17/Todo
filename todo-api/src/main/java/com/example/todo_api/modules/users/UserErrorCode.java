package com.example.todo_api.modules.users;

import org.springframework.http.HttpStatus;

import com.example.todo_api.core.exceptions.ErrorCode;

/** Mã lỗi của tài khoản / hồ sơ người dùng / avatar. */
public enum UserErrorCode implements ErrorCode {

    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Account is locked"),
    AVATAR_REQUIRED(HttpStatus.BAD_REQUEST, "Please choose an image file"),
    AVATAR_TOO_LARGE(HttpStatus.BAD_REQUEST, "Image must be 2 MB or smaller"),
    AVATAR_INVALID_TYPE(HttpStatus.BAD_REQUEST, "Use JPEG, PNG, WebP, or GIF"),
    AVATAR_READ_FAILED(HttpStatus.BAD_REQUEST, "Could not read uploaded file"),
    AVATAR_URL_TOO_LONG(HttpStatus.BAD_REQUEST,
            "Public URL is too long; use a shorter custom domain for R2_PUBLIC_URL");

    private final HttpStatus status;
    private final String message;

    UserErrorCode(HttpStatus status, String message) {
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
