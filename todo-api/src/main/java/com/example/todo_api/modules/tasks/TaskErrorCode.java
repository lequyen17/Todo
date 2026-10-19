package com.example.todo_api.modules.tasks;

import org.springframework.http.HttpStatus;

import com.example.todo_api.core.exceptions.ErrorCode;

/** Mã lỗi của task. */
public enum TaskErrorCode implements ErrorCode {

    // BR-02: task của người khác cũng trả về lỗi này, không tiết lộ task có tồn tại
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task not found"),

    // VR-10
    REMINDER_REQUIRES_DUE_DATE(HttpStatus.BAD_REQUEST, "Set a due date before adding a reminder"),
    REMINDER_IN_PAST(HttpStatus.BAD_REQUEST, "Reminder must be in the future"),
    REMINDER_AFTER_DUE_DATE(HttpStatus.BAD_REQUEST, "Reminder must not be later than the due date");

    private final HttpStatus status;
    private final String message;

    TaskErrorCode(HttpStatus status, String message) {
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
