package com.example.todo_api.modules.auth.session;

import com.example.todo_api.modules.auth.dto.TokenResponse;

/**
 * Kết quả đăng nhập service trả cho controller, không phải DTO của API.
 * refreshTokenPlain là token gốc: controller chỉ ghi vào HttpOnly cookie, không bao giờ trả trong body JSON.
 */
public record AuthSession(TokenResponse tokens, String refreshTokenPlain) {
}
