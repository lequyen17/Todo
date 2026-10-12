package com.example.todo_api.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ChallengeRequest(@NotBlank String challengeId) {
}
