package com.example.todo_api.modules.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.todo_api.modules.users.entity.User;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");
    private static final Duration TTL = Duration.ofDays(7);

    private final User user = User.registerWithPassword("a@example.com", "A", "hash", NOW);

    @Test
    void issue_usesTtlWhenSessionHasTimeLeft() {
        RefreshToken token = RefreshToken.issue(user, "h", UUID.randomUUID(), NOW, TTL, NOW.plus(Duration.ofDays(30)));

        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(TTL));
    }

    @Test
    void issue_neverOutlivesSession() {
        Instant sessionEnd = NOW.plus(Duration.ofDays(2));

        RefreshToken token = RefreshToken.issue(user, "h", UUID.randomUUID(), NOW, TTL, sessionEnd);

        assertThat(token.getExpiresAt()).isEqualTo(sessionEnd);
    }

    @Test
    void newToken_isUsableUntilExpiry() {
        RefreshToken token = RefreshToken.issue(user, "h", UUID.randomUUID(), NOW, TTL, Instant.MAX);

        assertThat(token.isUsedOrRevoked()).isFalse();
        assertThat(token.isExpired(NOW.plus(TTL).minusSeconds(1))).isFalse();
        assertThat(token.isExpired(NOW.plus(TTL))).isTrue();
    }

    @Test
    void rotateTo_marksOldTokenAsUsed() {
        UUID family = UUID.randomUUID();
        RefreshToken current = RefreshToken.issue(user, "h1", family, NOW, TTL, Instant.MAX);
        RefreshToken next = RefreshToken.issue(user, "h2", family, NOW, TTL, Instant.MAX);

        current.rotateTo(next, NOW);

        assertThat(current.isUsedOrRevoked()).isTrue();
        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(next.isUsedOrRevoked()).isFalse();
    }
}
