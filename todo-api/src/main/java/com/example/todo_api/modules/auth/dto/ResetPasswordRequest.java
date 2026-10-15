package com.example.todo_api.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank String token, @ValidPassword String newPassword) {
}
