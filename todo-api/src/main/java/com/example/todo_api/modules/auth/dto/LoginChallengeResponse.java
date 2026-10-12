package com.example.todo_api.modules.auth.dto;

public record LoginChallengeResponse(String challengeId, String email, long expiresIn) {
}
