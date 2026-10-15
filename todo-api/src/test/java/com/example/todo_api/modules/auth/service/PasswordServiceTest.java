package com.example.todo_api.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.infrastructure.cache.RateLimiter;
import com.example.todo_api.infrastructure.email.MailService;
import com.example.todo_api.modules.auth.dto.AuthSession;
import com.example.todo_api.modules.auth.dto.ChangePasswordRequest;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.repository.UserRepository;

class PasswordServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private SessionService sessionService;
    private PasswordService service;
    private User user;
    private AuthPrincipal principal;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        sessionService = mock(SessionService.class);
        AppProperties props = new AppProperties(null, null, null, null, null, null,
                new AppProperties.Google(null), new AppProperties.R2(null, null, null, null, null));
        service = new PasswordService(userRepository, passwordEncoder, mock(MailService.class),
                mock(RateLimiter.class), mock(StringRedisTemplate.class), mock(LoginAttemptService.class),
                sessionService, props);

        user = new User();
        user.setPassword("hash-of-old");
        principal = new AuthPrincipal(user, "jti-1", Instant.now().plusSeconds(600));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "hash-of-old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("hash-of-new");
    }

    @Test
    void change_success_revokesOldSessionsAndStartsNewOne() {
        AuthSession newSession = new AuthSession(null, "new-refresh");
        when(sessionService.start(any(), any())).thenReturn(newSession);

        AuthSession result = service.change(principal, new ChangePasswordRequest("old-pass", "new-pass"));

        assertThat(result).isSameAs(newSession);
        assertThat(user.getPassword()).isEqualTo("hash-of-new");
        assertThat(user.getPasswordChangedAt()).isNotNull();
        verify(sessionService).revokeAll(any(), any());
        verify(sessionService).revokeAccessToken(principal);
    }

    @Test
    void change_wrongCurrentPassword_changesNothing() {
        assertError(() -> service.change(principal, new ChangePasswordRequest("wrong", "new-pass")),
                "INVALID_CURRENT_PASSWORD");
        assertThat(user.getPassword()).isEqualTo("hash-of-old");
        verifyNoInteractions(sessionService);
    }

    @Test
    void change_samePassword_isRejected() {
        assertError(() -> service.change(principal, new ChangePasswordRequest("old-pass", "old-pass")),
                "SAME_PASSWORD");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void change_googleOnlyAccount_mustUseForgotPassword() {
        user.setPassword(null);

        assertError(() -> service.change(principal, new ChangePasswordRequest("x", "new-pass")),
                "PASSWORD_NOT_SET");
    }

    private static void assertError(Runnable call, String code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
