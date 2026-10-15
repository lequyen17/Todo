package com.example.todo_api.modules.auth.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.infrastructure.cache.RateLimiter;
import com.example.todo_api.infrastructure.email.MailService;
import com.example.todo_api.modules.auth.dto.AuthSession;
import com.example.todo_api.modules.auth.dto.LoginChallengeResponse;
import com.example.todo_api.modules.auth.dto.LoginRequest;
import com.example.todo_api.modules.auth.dto.LoginResult;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.repository.UserRepository;

/** Đăng nhập bằng mật khẩu, kèm bước OTP email (MFA) khi app.login.otp-enabled = true. */
@Service
public class LoginService {

    private static final String PURPOSE = "login";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final MailService mailService;
    private final RateLimiter rateLimiter;
    private final LoginAttemptService loginAttempts;
    private final SessionService sessionService;
    private final AppProperties props;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, OtpService otpService,
            MailService mailService, RateLimiter rateLimiter, LoginAttemptService loginAttempts,
            SessionService sessionService, AppProperties props) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.mailService = mailService;
        this.rateLimiter = rateLimiter;
        this.loginAttempts = loginAttempts;
        this.sessionService = sessionService;
        this.props = props;
    }

    public LoginResult login(LoginRequest req, String ip) {
        String email = AuthRules.normalizeEmail(req.email());
        rateLimiter.check(ip, "login", 20);
        loginAttempts.ensureNotLocked(email);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            loginAttempts.recordFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        AuthRules.ensureActive(user);
        loginAttempts.reset(email);

        if (!props.login().otpEnabled()) {
            Instant now = Instant.now();
            user.setLastLoginAt(now);
            userRepository.save(user);
            return LoginResult.authenticated(sessionService.start(user, now));
        }

        String challengeId = UUID.randomUUID().toString();
        String otp = otpService.create(key(challengeId), Map.of("user_id", user.getId().toString()));
        mailService.sendLoginOtp(user.getEmail(), user.getDisplayName(), otp);
        otpService.startCooldown(PURPOSE, challengeId);
        return LoginResult.challenge(
                new LoginChallengeResponse(challengeId, user.getEmail(), props.otp().ttl().toSeconds()));
    }

    public void resendOtp(String challengeId, String ip) {
        rateLimiter.check(ip, "login-resend", 10);
        Map<String, String> challenge = otpService.find(key(challengeId));
        if (challenge.isEmpty()) {
            throw challengeExpired();
        }
        otpService.ensureNoCooldown(PURPOSE, challengeId);
        User user = userRepository.findById(Long.valueOf(challenge.get("user_id")))
                .orElseThrow(LoginService::challengeExpired);
        String otp = otpService.regenerate(key(challengeId));
        mailService.sendLoginOtp(user.getEmail(), user.getDisplayName(), otp);
        otpService.startCooldown(PURPOSE, challengeId);
    }

    @Transactional
    public AuthSession verifyOtp(String challengeId, String otp, String ip) {
        rateLimiter.check(ip, "login-verify", 20);
        Map<String, String> challenge = otpService.verify(key(challengeId), otp);
        User user = userRepository.findById(Long.valueOf(challenge.get("user_id")))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Account not found"));
        AuthRules.ensureActive(user);
        Instant now = Instant.now();
        user.setLastLoginAt(now);
        return sessionService.start(user, now);
    }

    private static ApiException challengeExpired() {
        return new ApiException(HttpStatus.BAD_REQUEST, "CHALLENGE_EXPIRED",
                "Verification session has expired, please sign in again");
    }

    private static String key(String challengeId) {
        return "mfa_challenge:" + challengeId;
    }
}
