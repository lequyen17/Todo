package com.example.todo_api.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.core.security.AccessTokenBlacklist;
import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.core.security.JwtService;
import com.example.todo_api.core.utils.Tokens;
import com.example.todo_api.modules.auth.dto.AuthSession;
import com.example.todo_api.modules.auth.entity.RefreshToken;
import com.example.todo_api.modules.auth.repository.RefreshTokenRepository;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.entity.UserStatus;

class SessionServiceTest {

    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Duration SESSION_MAX_AGE = Duration.ofDays(30);

    private RefreshTokenRepository repo;
    private AccessTokenBlacklist blacklist;
    private SessionService service;
    private User user;

    @BeforeEach
    void setUp() {
        repo = mock(RefreshTokenRepository.class);
        blacklist = mock(AccessTokenBlacklist.class);
        JwtService jwtService = mock(JwtService.class);
        AppProperties props = new AppProperties(null, null,
                new AppProperties.Jwt("secret", Duration.ofMinutes(15), REFRESH_TTL, SESSION_MAX_AGE),
                null, null, null, new AppProperties.Google(null),
                new AppProperties.R2(null, null, null, null, null));
        service = new SessionService(repo, jwtService, blacklist, props);
        user = new User();

        when(repo.saveAndFlush(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.issue(any())).thenReturn(
                new JwtService.AccessToken("access-jwt", "jti", Instant.now().plusSeconds(900)));
    }

    @Test
    void start_storesOnlyHashOfRefreshToken() {
        AuthSession session = service.start(user, Instant.now());

        RefreshToken saved = captureSaved();
        assertThat(session.tokens().accessToken()).isEqualTo("access-jwt");
        assertThat(saved.getToken())
                .isEqualTo(Tokens.sha256(session.refreshTokenPlain()))
                .isNotEqualTo(session.refreshTokenPlain());
    }

    @Test
    void refresh_rotatesTokenWithinSameFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken current = storedToken("old-plain", family, Instant.now().plus(Duration.ofDays(1)));
        when(repo.findFamilyStartedAt(family)).thenReturn(Instant.now().minus(Duration.ofHours(1)));

        AuthSession session = service.refresh("old-plain");

        assertThat(session.refreshTokenPlain()).isNotEqualTo("old-plain");
        assertThat(current.getRevokedAt()).isNotNull();
        assertThat(captureSaved().getFamilyId()).isEqualTo(family);
    }

    @Test
    void refresh_reusedToken_revokesWholeFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken current = storedToken("stolen", family, Instant.now().plus(Duration.ofDays(1)));
        current.setRevokedAt(Instant.now().minusSeconds(60));

        assertInvalidRefreshToken(() -> service.refresh("stolen"));
        verify(repo).revokeFamily(eq(family), any());
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void refresh_expiredToken_isRejected() {
        storedToken("expired", UUID.randomUUID(), Instant.now().minusSeconds(1));

        assertInvalidRefreshToken(() -> service.refresh("expired"));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void refresh_unknownToken_isRejected() {
        when(repo.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertInvalidRefreshToken(() -> service.refresh("unknown"));
    }

    @Test
    void refresh_afterSessionMaxAge_revokesFamily() {
        UUID family = UUID.randomUUID();
        storedToken("old", family, Instant.now().plus(Duration.ofDays(1)));
        when(repo.findFamilyStartedAt(family)).thenReturn(Instant.now().minus(SESSION_MAX_AGE).minusSeconds(1));

        assertInvalidRefreshToken(() -> service.refresh("old"));
        verify(repo).revokeFamily(eq(family), any());
    }

    @Test
    void refresh_nearSessionEnd_capsExpiryAtSessionEnd() {
        UUID family = UUID.randomUUID();
        Instant startedAt = Instant.now().minus(SESSION_MAX_AGE).plus(Duration.ofDays(1));
        storedToken("old", family, Instant.now().plus(Duration.ofDays(1)));
        when(repo.findFamilyStartedAt(family)).thenReturn(startedAt);

        service.refresh("old");

        assertThat(captureSaved().getExpiresAt()).isEqualTo(startedAt.plus(SESSION_MAX_AGE));
    }

    @Test
    void refresh_lockedUser_revokesFamily() {
        UUID family = UUID.randomUUID();
        storedToken("old", family, Instant.now().plus(Duration.ofDays(1)));
        when(repo.findFamilyStartedAt(family)).thenReturn(Instant.now());
        user.setStatus(UserStatus.LOCKED);

        assertThatThrownBy(() -> service.refresh("old"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("ACCOUNT_LOCKED"));
        verify(repo).revokeFamily(eq(family), any());
    }

    @Test
    void logout_blacklistsAccessTokenAndRevokesFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken stored = new RefreshToken(user, Tokens.sha256("plain"), family, Instant.now().plusSeconds(60));
        when(repo.findByTokenHash(Tokens.sha256("plain"))).thenReturn(Optional.of(stored));
        Instant accessExpiresAt = Instant.now().plusSeconds(600).truncatedTo(ChronoUnit.SECONDS);
        AuthPrincipal principal = new AuthPrincipal(user, "jti-1", accessExpiresAt);

        service.logout(principal, "plain");

        verify(blacklist).add("jti-1", accessExpiresAt);
        verify(repo).revokeFamily(eq(family), any());
    }

    @Test
    void logout_withoutPrincipal_stillRevokesFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken stored = new RefreshToken(user, Tokens.sha256("plain"), family, Instant.now().plusSeconds(60));
        when(repo.findByTokenHash(Tokens.sha256("plain"))).thenReturn(Optional.of(stored));

        service.logout(null, "plain");

        verify(blacklist, never()).add(any(), any());
        verify(repo).revokeFamily(eq(family), any());
    }

    private RefreshToken storedToken(String plain, UUID family, Instant expiresAt) {
        RefreshToken token = new RefreshToken(user, Tokens.sha256(plain), family, expiresAt);
        when(repo.findByTokenHashForUpdate(Tokens.sha256(plain))).thenReturn(Optional.of(token));
        return token;
    }

    private RefreshToken captureSaved() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static void assertInvalidRefreshToken(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"));
    }
}
