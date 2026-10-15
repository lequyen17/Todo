package com.example.todo_api.modules.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
