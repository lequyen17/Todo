package com.example.todo_api.modules.auth.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.infrastructure.cache.RateLimiter;
import com.example.todo_api.infrastructure.email.MailService;
import com.example.todo_api.modules.auth.dto.AuthSession;
import com.example.todo_api.modules.auth.dto.RegisterRequest;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.repository.UserRepository;

/** Đăng ký: lưu tạm thông tin (mật khẩu đã hash) vào Redis, chỉ tạo user sau khi xác minh OTP email. */
@Service
public class RegistrationService {

    private static final String PURPOSE = "reg";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final MailService mailService;
    private final RateLimiter rateLimiter;
    private final SessionService sessionService;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            OtpService otpService, MailService mailService, RateLimiter rateLimiter,
            SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.mailService = mailService;
        this.rateLimiter = rateLimiter;
        this.sessionService = sessionService;
    }

    public void register(RegisterRequest req, String ip) {
        String email = AuthRules.normalizeEmail(req.email());
        rateLimiter.check(ip, "register", 10);
        rateLimiter.check(email, "register", 3);
        if (userRepository.existsByEmail(email)) {
            throw emailExists();
        }
        otpService.ensureNoCooldown(PURPOSE, email);

        String displayName = req.displayName().trim();
        String otp = otpService.create(key(email), Map.of(
                "display_name", displayName,
                "password", passwordEncoder.encode(req.password())));
        mailService.sendRegistrationOtp(email, displayName, otp);
        otpService.startCooldown(PURPOSE, email);
    }

    public void resendOtp(String rawEmail, String ip) {
        String email = AuthRules.normalizeEmail(rawEmail);
        rateLimiter.check(ip, "register-resend", 10);
        Map<String, String> pending = otpService.find(key(email));
        if (pending.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REGISTRATION_EXPIRED",
                    "Registration session has expired, please register again");
        }
        otpService.ensureNoCooldown(PURPOSE, email);
        String otp = otpService.regenerate(key(email));
        mailService.sendRegistrationOtp(email, pending.get("display_name"), otp);
        otpService.startCooldown(PURPOSE, email);
    }

    @Transactional
    public AuthSession verify(String rawEmail, String otp, String ip) {
        String email = AuthRules.normalizeEmail(rawEmail);
        rateLimiter.check(ip, "register-verify", 20);
        Map<String, String> pending = otpService.verify(key(email), otp);
        if (userRepository.existsByEmail(email)) {
            throw emailExists();
        }
        Instant now = Instant.now();
        User user = new User();
        user.setEmail(email);
        user.setPassword(pending.get("password"));
        user.setDisplayName(pending.get("display_name"));
        user.setEmailVerifiedAt(now);
        user.setLastLoginAt(now);
        userRepository.save(user);
        return sessionService.start(user, now);
    }

    private static ApiException emailExists() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email is already registered");
    }

    private static String key(String email) {
        return "reg_otp:" + email;
    }
}
