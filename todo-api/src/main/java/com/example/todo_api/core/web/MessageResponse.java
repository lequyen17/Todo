package com.example.todo_api.core.web;

/** Body thành công chỉ có một thông báo, vd. {"message": "A verification code has been sent to your email"}. */
public record MessageResponse(String message) {
}
