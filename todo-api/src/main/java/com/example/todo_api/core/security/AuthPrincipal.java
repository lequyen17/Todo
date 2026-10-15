package com.example.todo_api.core.security;

import java.time.Instant;

/**
 * Principal gắn vào SecurityContext sau khi access token hợp lệ.
 * Chỉ chứa dữ liệu đơn giản; module nào cần entity User thì tự tải theo userId.
 */
public record AuthPrincipal(Long userId, String email, String role, String jti, Instant accessExpiresAt) {
}
