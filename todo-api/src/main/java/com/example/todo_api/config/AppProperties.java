package com.example.todo_api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        Mail mail,
        Jwt jwt,
        Otp otp,
        PasswordReset passwordReset,
        Login login) {

    public record Mail(String from, boolean enabled) {
    }

    public record Jwt(String secret, Duration accessTtl, Duration refreshTtl, Duration sessionMaxAge) {
    }

    public record Otp(Duration ttl, int maxAttempts, Duration resendCooldown) {
    }

    public record PasswordReset(Duration ttl) {
    }

    /** otpEnabled = false: đăng nhập chỉ cần mật khẩu, bỏ bước OTP email. */
    public record Login(int maxFailures, Duration lockDuration, boolean otpEnabled) {
    }
}
