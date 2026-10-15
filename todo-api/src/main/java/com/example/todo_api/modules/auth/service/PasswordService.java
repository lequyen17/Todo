package com.example.todo_api.modules.auth.service;

import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.core.utils.Tokens;
import com.example.todo_api.infrastructure.cache.RateLimiter;
import com.example.todo_api.infrastructure.email.MailService;
import com.example.todo_api.modules.auth.dto.AuthSession;
import com.example.todo_api.modules.auth.dto.ChangePasswordRequest;
import com.example.todo_api.modules.auth.dto.ResetPasswordRequest;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.entity.UserStatus;
import com.example.todo_api.modules.users.repository.UserRepository;

/** Đổi mật khẩu, quên mật khẩu (gửi link), đặt lại mật khẩu. Mọi lần đổi đều thu hồi các phiên cũ. */
@Service
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final RateLimiter rateLimiter;
    private final StringRedisTemplate redis;
    private final LoginAttemptService loginAttempts;
    private final SessionService sessionService;
    private final AppProperties props;

    public PasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder, MailService mailService,
            RateLimiter rateLimiter, StringRedisTemplate redis, LoginAttemptService loginAttempts,
            SessionService sessionService, AppProperties props) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.rateLimiter = rateLimiter;
        this.redis = redis;
        this.loginAttempts = loginAttempts;
        this.sessionService = sessionService;
        this.props = props;
    }

    /** Đổi mật khẩu -> thu hồi mọi phiên, cấp phiên mới cho thiết bị hiện tại. */
    @Transactional
    public AuthSession change(AuthPrincipal principal, ChangePasswordRequest req) {
        rateLimiter.check("user-" + principal.user().getId(), "change-password", 5);
        User user = userRepository.findById(principal.user().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
        if (user.getPassword() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_NOT_SET",
                    "This account signs in with Google and has no password yet. Use Forgot password to set one");
        }
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_PASSWORD", "New password must be different from the current one");
        }
        Instant now = Instant.now();
        updatePassword(user, req.newPassword(), now);
        sessionService.revokeAccessToken(principal);
        return sessionService.start(user, now);
    }

    /** Luôn trả về thành công để không lộ email nào đã đăng ký. */
    public void forgot(String rawEmail, String ip) {
        String email = AuthRules.normalizeEmail(rawEmail);
        rateLimiter.check(ip, "forgot-password", 10);
        rateLimiter.check(email, "forgot-password", 3);
        userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    String token = Tokens.random(32);
                    redis.opsForValue().set(resetKey(token), user.getId().toString(), props.passwordReset().ttl());
                    String link = props.frontendUrl() + "/reset-password?token=" + token;
                    mailService.sendPasswordReset(user.getEmail(), user.getDisplayName(), link);
                });
    }

    @Transactional
    public void reset(ResetPasswordRequest req, String ip) {
        rateLimiter.check(ip, "reset-password", 10);
        String userId = redis.opsForValue().getAndDelete(resetKey(req.token()));
        if (userId == null) {
            throw resetTokenInvalid();
        }
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(PasswordService::resetTokenInvalid);
        updatePassword(user, req.newPassword(), Instant.now());
        loginAttempts.reset(user.getEmail());
    }

    private void updatePassword(User user, String rawPassword, Instant now) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPasswordChangedAt(now);
        userRepository.save(user);
        sessionService.revokeAll(user, now);
    }

    private static ApiException resetTokenInvalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "RESET_TOKEN_INVALID",
                "Password reset link is invalid or has expired");
    }

    private static String resetKey(String token) {
        return "pwd_reset:" + Tokens.sha256(token);
    }
}
