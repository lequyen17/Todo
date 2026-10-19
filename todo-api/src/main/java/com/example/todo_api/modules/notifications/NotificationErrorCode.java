package com.example.todo_api.modules.notifications;

import org.springframework.http.HttpStatus;

import com.example.todo_api.core.exceptions.ErrorCode;

/** Mã lỗi của thông báo trong web. */
public enum NotificationErrorCode implements ErrorCode {

    // BR-03: thông báo của người khác cũng trả về lỗi này
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification not found");

    private final HttpStatus status;
    private final String message;

    NotificationErrorCode(HttpStatus status, String message) {
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
