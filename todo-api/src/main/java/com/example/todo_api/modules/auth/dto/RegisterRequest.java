package com.example.todo_api.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.todo_api.modules.users.entity.User;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @ValidPassword String password,
        @NotBlank
        @Size(min = 2, max = User.DISPLAY_NAME_MAX_LENGTH, message = "Display name must be 2 to 50 characters")
        String displayName) {

    /** Bỏ khoảng trắng đầu/cuối trước khi validate, để "  a  " không lọt qua giới hạn tối thiểu 2 ký tự. */
    public RegisterRequest {
        displayName = displayName == null ? null : displayName.trim();
    }
}
