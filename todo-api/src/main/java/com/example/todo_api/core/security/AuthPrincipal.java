package com.example.todo_api.core.security;

import java.time.Instant;

import com.example.todo_api.modules.users.entity.User;

/** Principal gắn vào SecurityContext sau khi access token hợp lệ. */
public record AuthPrincipal(User user, String jti, Instant accessExpiresAt) {
}
