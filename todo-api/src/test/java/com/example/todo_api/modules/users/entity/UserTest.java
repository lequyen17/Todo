package com.example.todo_api.modules.users.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.users.UserErrorCode;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    @Test
    void registerWithPassword_normalizesEmailAndMarksVerified() {
        User user = User.registerWithPassword("  Alice@Example.COM ", "  Alice  ", "hash", NOW);

        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(user.hasPassword()).isTrue();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void registerWithGoogle_hasNoPasswordAndFallsBackToEmailName() {
        User user = User.registerWithGoogle("bob@example.com", "  ", NOW);

        assertThat(user.hasPassword()).isFalse();
        assertThat(user.getDisplayName()).isEqualTo("bob");
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void displayName_isCappedAtMaxLength() {
        User user = User.registerWithGoogle("c@example.com", "x".repeat(80), NOW);

        assertThat(user.getDisplayName()).hasSize(User.DISPLAY_NAME_MAX_LENGTH);
    }

    @Test
    void changePassword_alwaysRecordsChangeTime() {
        User user = User.registerWithPassword("a@example.com", "A", "old", NOW);
        Instant later = NOW.plusSeconds(60);

        user.changePassword("new", later);

        assertThat(user.getPassword()).isEqualTo("new");
        assertThat(user.getPasswordChangedAt()).isEqualTo(later);
    }

    @Test
    void lockedUser_failsEnsureActive() {
        User user = User.registerWithPassword("a@example.com", "A", "hash", NOW);
        assertThatCode(user::ensureActive).doesNotThrowAnyException();

        user.lock();

        assertThat(user.isActive()).isFalse();
        assertThatThrownBy(user::ensureActive)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo(UserErrorCode.ACCOUNT_LOCKED));
    }

    @Test
    void markEmailVerified_keepsFirstVerificationTime() {
        User user = User.registerWithGoogle("a@example.com", "A", NOW);

        user.markEmailVerified(NOW.plusSeconds(3600));

        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void useAvatarIfMissing_neverOverridesOrAcceptsTooLongUrl() {
        User user = User.registerWithGoogle("a@example.com", "A", NOW);

        user.useAvatarIfMissing("https://x/" + "a".repeat(User.AVATAR_URL_MAX_LENGTH));
        assertThat(user.getAvatarUrl()).isNull();

        user.useAvatarIfMissing("https://google/pic.png");
        user.useAvatarIfMissing("https://google/other.png");
        assertThat(user.getAvatarUrl()).isEqualTo("https://google/pic.png");
    }

    @Test
    void changeAvatar_rejectsTooLongUrl() {
        User user = User.registerWithGoogle("a@example.com", "A", NOW);

        assertThatThrownBy(() -> user.changeAvatar("https://x/" + "a".repeat(User.AVATAR_URL_MAX_LENGTH)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo(UserErrorCode.AVATAR_URL_TOO_LONG));
        assertThat(user.getAvatarUrl()).isNull();
    }
}
