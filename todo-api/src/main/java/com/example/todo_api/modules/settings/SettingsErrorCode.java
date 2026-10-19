package com.example.todo_api.modules.settings;

import org.springframework.http.HttpStatus;

import com.example.todo_api.core.exceptions.ErrorCode;

/** Mã lỗi của cài đặt nhắc nhở. */
public enum SettingsErrorCode implements ErrorCode {

    // FR-05.2: bật nhắc nhở thì phải chọn ít nhất một kênh
    REMINDER_CHANNEL_REQUIRED(HttpStatus.BAD_REQUEST, "Choose at least one channel to receive reminders");

    private final HttpStatus status;
    private final String message;

    SettingsErrorCode(HttpStatus status, String message) {
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
