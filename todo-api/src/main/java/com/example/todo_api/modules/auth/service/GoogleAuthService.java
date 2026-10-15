package com.example.todo_api.modules.auth.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.infrastructure.cache.RateLimiter;
import com.example.todo_api.infrastructure.google.GoogleTokenVerifier;
import com.example.todo_api.infrastructure.google.GoogleTokenVerifier.GoogleProfile;
import com.example.todo_api.modules.auth.dto.AuthSession;
import com.example.todo_api.modules.auth.dto.GoogleConfigResponse;
import com.example.todo_api.modules.auth.entity.UserIdentity;
import com.example.todo_api.modules.auth.repository.UserIdentityRepository;
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

    public GoogleAuthService(GoogleTokenVerifier googleVerifier, UserRepository userRepository,
            UserIdentityRepository identityRepository, RateLimiter rateLimiter, SessionService sessionService,
            AppProperties props) {
        this.googleVerifier = googleVerifier;
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.rateLimiter = rateLimiter;
        this.sessionService = sessionService;
        this.props = props;
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
                    identity.setProviderEmail(profile.email());
                    return identity.getUser();
                })
                .orElseGet(() -> linkOrCreateUser(profile, now));
        AuthRules.ensureActive(user);

        if (user.getAvatarUrl() == null && profile.picture() != null && profile.picture().length() <= 500) {
            user.setAvatarUrl(profile.picture());
        }
        user.setLastLoginAt(now);
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
            throw new ApiException(HttpStatus.FORBIDDEN, "GOOGLE_EMAIL_UNVERIFIED",
                    "Your Google account email is not verified");
        }
        String email = AuthRules.normalizeEmail(profile.email());
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setDisplayName(displayName(profile, email));
            user.setEmailVerifiedAt(now);
            userRepository.save(user);
        } else {
            if (identityRepository.existsByUserIdAndProvider(user.getId(), UserIdentity.Provider.GOOGLE)) {
                throw new ApiException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_MISMATCH",
                        "This email is already linked to a different Google account");
            }
            if (user.getEmailVerifiedAt() == null) {
                user.setEmailVerifiedAt(now);
            }
        }
        identityRepository.save(new UserIdentity(user, UserIdentity.Provider.GOOGLE, profile.sub(), profile.email()));
        return user;
    }

    private static String displayName(GoogleProfile profile, String email) {
        String name = profile.name() == null ? "" : profile.name().trim();
        if (name.isEmpty()) {
            name = email.substring(0, email.indexOf('@'));
        }
        return name.length() > 50 ? name.substring(0, 50) : name;
    }
}
