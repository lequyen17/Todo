package com.example.todo_api.modules.auth.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.infrastructure.cache.RateLimiter;
import com.example.todo_api.infrastructure.google.GoogleTokenVerifier.GoogleProfile;
import com.example.todo_api.infrastructure.google.GoogleTokenVerifier;
import com.example.todo_api.modules.auth.AuthErrorCode;
import com.example.todo_api.modules.auth.dto.GoogleConfigResponse;
import com.example.todo_api.modules.auth.entity.UserIdentity;
import com.example.todo_api.modules.auth.repository.UserIdentityRepository;
import com.example.todo_api.modules.auth.session.AuthSession;
import com.example.todo_api.modules.settings.entity.UserSettings;
import com.example.todo_api.modules.settings.repository.UserSettingsRepository;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.repository.UserRepository;

/** Đăng nhập bằng Google ID token (Google Identity Services). */
@Service
public class GoogleAuthService {

    private final GoogleTokenVerifier googleVerifier;
    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final RateLimiter rateLimiter;
    private final SessionService sessionService;
    private final AppProperties props;
    private final UserSettingsRepository settingsRepository;

    public GoogleAuthService(GoogleTokenVerifier googleVerifier, UserRepository userRepository,
            UserIdentityRepository identityRepository, RateLimiter rateLimiter, SessionService sessionService,
            AppProperties props, UserSettingsRepository settingsRepository) {
        this.googleVerifier = googleVerifier;
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.rateLimiter = rateLimiter;
        this.sessionService = sessionService;
        this.props = props;
        this.settingsRepository = settingsRepository;
    }

    /**
     * Google đã xác thực người dùng nên bỏ qua bước OTP email.
     * Thứ tự tìm tài khoản: theo Google sub đã liên kết -> theo email (tự liên kết) -> tạo tài khoản mới.
     */
    @Transactional
    public AuthSession login(String credential, String ip) {
        rateLimiter.check(ip, "login-google", 20);
        GoogleProfile profile = googleVerifier.verify(credential);
        Instant now = Instant.now();

        User user = identityRepository.findByProviderAndProviderUserId(UserIdentity.Provider.GOOGLE, profile.sub())
                .map(identity -> {
                    identity.updateProviderEmail(profile.email());
                    return identity.getUser();
                })
                .orElseGet(() -> linkOrCreateUser(profile, now));
        user.ensureActive();
        user.useAvatarIfMissing(profile.picture());
        user.recordLogin(now);
        userRepository.save(user);
        return sessionService.start(user, now);
    }

    public GoogleConfigResponse config() {
        AppProperties.Google google = props.google();
        return new GoogleConfigResponse(google.enabled(), google.enabled() ? google.clientId() : null);
    }

    private User linkOrCreateUser(GoogleProfile profile, Instant now) {
        // Chỉ tin email Google đã xác minh, nếu không ai cũng có thể chiếm tài khoản theo email
        if (profile.email() == null || !profile.emailVerified()) {
            throw new ApiException(AuthErrorCode.GOOGLE_EMAIL_UNVERIFIED);
        }
        String email = User.normalizeEmail(profile.email());
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = userRepository.save(User.registerWithGoogle(email, profile.name(), now));
            settingsRepository.save(UserSettings.defaultsFor(user.getId()));
        } else {
            if (identityRepository.existsByUserIdAndProvider(user.getId(), UserIdentity.Provider.GOOGLE)) {
                throw new ApiException(AuthErrorCode.GOOGLE_ACCOUNT_MISMATCH);
            }
            user.markEmailVerified(now);
        }
        identityRepository.save(new UserIdentity(user, UserIdentity.Provider.GOOGLE, profile.sub(), profile.email()));
        return user;
    }
}
