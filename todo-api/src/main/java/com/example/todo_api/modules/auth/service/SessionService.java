package com.example.todo_api.modules.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.core.security.AccessTokenBlacklist;
import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.core.utils.Tokens;
import com.example.todo_api.modules.auth.AuthErrorCode;
import com.example.todo_api.modules.auth.dto.TokenResponse;
import com.example.todo_api.modules.auth.entity.RefreshToken;
import com.example.todo_api.modules.auth.repository.RefreshTokenRepository;
import com.example.todo_api.modules.auth.security.JwtIssuer;
import com.example.todo_api.modules.auth.session.AuthSession;
import com.example.todo_api.modules.users.entity.User;

/**
 * Vòng đời phiên đăng nhập: cấp access + refresh token, rotate refresh token, thu hồi khi logout / đổi mật khẩu.
 * Mọi luồng đăng nhập (mật khẩu, OTP, Google, đăng ký) đều kết thúc bằng {@link #start(User, Instant)}.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtIssuer jwtIssuer;
    private final AccessTokenBlacklist blacklist;
    private final AppProperties props;

    public SessionService(RefreshTokenRepository refreshTokenRepository, JwtIssuer jwtIssuer,
            AccessTokenBlacklist blacklist, AppProperties props) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtIssuer = jwtIssuer;
        this.blacklist = blacklist;
        this.props = props;
    }

    /** Mở phiên mới (family mới) cho user vừa xác thực xong. */
    public AuthSession start(User user, Instant now) {
        IssuedRefreshToken issued = createRefreshToken(user, UUID.randomUUID(), now, now);
        return buildSession(user, issued);
    }

    /**
     * Rotate: token cũ được đánh dấu replaced_by_id + revoked_at, token mới là dòng mới cùng family_id.
     * Không rollback khi ném lỗi để việc thu hồi family (phát hiện dùng lại token) vẫn được lưu.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthSession refresh(String plainToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(plainToken))
                .orElseThrow(() -> new ApiException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        Instant now = Instant.now();

        if (current.isUsedOrRevoked()) {
            // Token đã dùng rồi mà vẫn bị gửi lại -> có thể bị đánh cắp, thu hồi cả family
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected, revoked family {} of user {}",
                    current.getFamilyId(), current.getUser().getId());
            throw new ApiException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (current.isExpired(now)) {
            throw new ApiException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        Instant sessionStartedAt = refreshTokenRepository.findFamilyStartedAt(current.getFamilyId());
        if (!sessionStartedAt.plus(props.jwt().sessionMaxAge()).isAfter(now)) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new ApiException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        User user = current.getUser();
        if (!user.isActive()) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            user.ensureActive();
        }

        IssuedRefreshToken next = createRefreshToken(user, current.getFamilyId(), now, sessionStartedAt);
        current.rotateTo(next.entity(), now);
        return buildSession(user, next);
    }

    /** Chặn access token hiện tại và thu hồi cả family của refresh token. */
    @Transactional
    public void logout(AuthPrincipal principal, String plainRefreshToken) {
        if (principal != null) {
            revokeAccessToken(principal);
        }
        if (plainRefreshToken != null && !plainRefreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(hash(plainRefreshToken))
                    .ifPresent(t -> refreshTokenRepository.revokeFamily(t.getFamilyId(), Instant.now()));
        }
    }

    /** Đưa access token vào blacklist đến khi nó tự hết hạn. */
    public void revokeAccessToken(AuthPrincipal principal) {
        blacklist.add(principal.jti(), principal.accessExpiresAt());
    }

    /** Thu hồi mọi refresh token của user (đăng xuất mọi thiết bị). */
    public void revokeAll(User user, Instant now) {
        refreshTokenRepository.revokeAllForUser(user.getId(), now);
    }

    /** Phiên tính từ token đầu tiên trong family; hết sessionMaxAge thì phải đăng nhập lại. */
    private IssuedRefreshToken createRefreshToken(User user, UUID familyId, Instant now, Instant sessionStartedAt) {
        String plain = Tokens.random(32);
        Instant sessionEnd = sessionStartedAt.plus(props.jwt().sessionMaxAge());
        RefreshToken saved = refreshTokenRepository.saveAndFlush(RefreshToken.issue(
                user, hash(plain), familyId, now, props.jwt().refreshTtl(), sessionEnd));
        return new IssuedRefreshToken(saved, plain);
    }

    private AuthSession buildSession(User user, IssuedRefreshToken refreshToken) {
        JwtIssuer.AccessToken access = jwtIssuer.issue(user);
        TokenResponse body = new TokenResponse(access.value(), "Bearer", props.jwt().accessTtl().toSeconds());
        return new AuthSession(body, refreshToken.plainValue());
    }

    private static String hash(String plainToken) {
        return Tokens.sha256(plainToken);
    }

    private record IssuedRefreshToken(RefreshToken entity, String plainValue) {
    }
}
