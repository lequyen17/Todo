package com.example.todo_api.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(@NotBlank String currentPassword, @ValidPassword String newPassword) {
}
