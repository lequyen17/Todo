package com.example.todo_api.modules.auth;

import org.springframework.http.HttpStatus;

import com.example.todo_api.core.exceptions.ErrorCode;

/** Mã lỗi của đăng ký / đăng nhập / phiên / mật khẩu. */
public enum AuthErrorCode implements ErrorCode {

    // Đăng ký
    EMAIL_EXISTS(HttpStatus.CONFLICT, "Email is already registered"),
    REGISTRATION_EXPIRED(HttpStatus.BAD_REQUEST, "Registration session has expired, please register again"),

    // Đăng nhập
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    LOGIN_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts, please try again in %d minutes"),
    CHALLENGE_EXPIRED(HttpStatus.BAD_REQUEST, "Verification session has expired, please sign in again"),

    // OTP
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "Verification code has expired, please request a new one"),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "Incorrect verification code, %d attempt(s) left"),
    OTP_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect attempts, please start over"),
    OTP_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "Please wait %d seconds before requesting a new code"),

    // Google
    GOOGLE_EMAIL_UNVERIFIED(HttpStatus.FORBIDDEN, "Your Google account email is not verified"),
    GOOGLE_ACCOUNT_MISMATCH(HttpStatus.CONFLICT, "This email is already linked to a different Google account"),

    // Phiên
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Session has expired, please sign in again"),

    // Mật khẩu
    PASSWORD_NOT_SET(HttpStatus.BAD_REQUEST,
            "This account signs in with Google and has no password yet. Use Forgot password to set one"),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "Current password is incorrect"),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "New password must be different from the current one"),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Password reset link is invalid or has expired");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
