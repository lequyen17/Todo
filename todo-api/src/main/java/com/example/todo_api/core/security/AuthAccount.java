package com.example.todo_api.core.security;

import java.time.Instant;

/** Thông tin tài khoản tối thiểu mà JwtAuthFilter cần để quyết định có chấp nhận access token hay không. */
public record AuthAccount(Long id, String email, String role, boolean active, Instant passwordChangedAt) {
}
