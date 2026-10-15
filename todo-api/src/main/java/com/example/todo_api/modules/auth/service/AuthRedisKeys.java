package com.example.todo_api.modules.auth.service;

import com.example.todo_api.core.utils.Tokens;

/**
 * Toàn bộ Redis key của module auth (đổi prefix ở đây = dữ liệu cũ trong Redis bị bỏ qua).
 * Key của core/infrastructure nằm ở class sở hữu nó: rate:* (RateLimiter), access_blacklist:* (AccessTokenBlacklist).
 */
final class AuthRedisKeys {

    /** Mục đích OTP, dùng trong key cooldown gửi lại mã. */
    static final String PURPOSE_REGISTER = "reg";
    static final String PURPOSE_LOGIN = "login";

    private AuthRedisKeys() {
    }

    /** Hash: thông tin đăng ký chờ xác minh (display_name, password đã hash) + OTP. */
    static String registration(String email) {
        return "reg_otp:" + email;
    }

    /** Hash: phiên đăng nhập chờ nhập OTP (user_id) + OTP. */
    static String loginChallenge(String challengeId) {
        return "mfa_challenge:" + challengeId;
    }

    /** Counter: số lần sai mật khẩu liên tiếp. */
    static String loginFailures(String email) {
        return "login_fail:" + email;
    }

    /** Chặn gửi lại OTP liên tục. */
    static String otpCooldown(String purpose, String id) {
        return "otp_cooldown:" + purpose + ":" + id;
    }

    /** user_id của link đặt lại mật khẩu; chỉ lưu hash của token trong link. */
    static String passwordReset(String token) {
        return "pwd_reset:" + Tokens.sha256(token);
    }
}
