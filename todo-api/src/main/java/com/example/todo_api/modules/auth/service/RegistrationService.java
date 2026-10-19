package com.example.todo_api.modules.auth.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.infrastructure.cache.RateLimiter;
import com.example.todo_api.infrastructure.email.MailService;
import com.example.todo_api.modules.auth.AuthErrorCode;
import com.example.todo_api.modules.auth.dto.RegisterRequest;
import com.example.todo_api.modules.auth.session.AuthSession;
import com.example.todo_api.modules.settings.entity.UserSettings;
import com.example.todo_api.modules.settings.repository.UserSettingsRepository;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.repository.UserRepository;

/** Đăng ký: lưu tạm thông tin (mật khẩu đã hash) vào Redis, chỉ tạo user sau khi xác minh OTP email. */
@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final MailService mailService;
    private final RateLimiter rateLimiter;
    private final SessionService sessionService;
    private final UserSettingsRepository settingsRepository;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            OtpService otpService, MailService mailService, RateLimiter rateLimiter,
            SessionService sessionService, UserSettingsRepository settingsRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.mailService = mailService;
        this.rateLimiter = rateLimiter;
        this.sessionService = sessionService;
        this.settingsRepository = settingsRepository;
    }

    public void register(RegisterRequest req, String ip) {
        String email = User.normalizeEmail(req.email());
        rateLimiter.check(ip, "register", 10);
        rateLimiter.check(email, "register", 3);
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(AuthErrorCode.EMAIL_EXISTS);
        }
        otpService.ensureNoCooldown(AuthRedisKeys.PURPOSE_REGISTER, email);

        String displayName = req.displayName();
        String otp = otpService.create(AuthRedisKeys.registration(email), Map.of(
                "display_name", displayName,
                "password", passwordEncoder.encode(req.password())));
        mailService.sendRegistrationOtp(email, displayName, otp);
        otpService.startCooldown(AuthRedisKeys.PURPOSE_REGISTER, email);
    }

    public void resendOtp(String rawEmail, String ip) {
        String email = User.normalizeEmail(rawEmail);
        rateLimiter.check(ip, "register-resend", 10);
        Map<String, String> pending = otpService.find(AuthRedisKeys.registration(email));
        if (pending.isEmpty()) {
            throw new ApiException(AuthErrorCode.REGISTRATION_EXPIRED);
        }
        otpService.ensureNoCooldown(AuthRedisKeys.PURPOSE_REGISTER, email);
        String otp = otpService.regenerate(AuthRedisKeys.registration(email));
        mailService.sendRegistrationOtp(email, pending.get("display_name"), otp);
        otpService.startCooldown(AuthRedisKeys.PURPOSE_REGISTER, email);
    }

    @Transactional
    public AuthSession verify(String rawEmail, String otp, String ip) {
        String email = User.normalizeEmail(rawEmail);
        rateLimiter.check(ip, "register-verify", 20);
        Map<String, String> pending = otpService.verify(AuthRedisKeys.registration(email), otp);
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(AuthErrorCode.EMAIL_EXISTS);
        }
        Instant now = Instant.now();
        User user = User.registerWithPassword(email, pending.get("display_name"), pending.get("password"), now);
        user.recordLogin(now);
        userRepository.save(user);
        settingsRepository.save(UserSettings.defaultsFor(user.getId()));
        return sessionService.start(user, now);
    }
}
