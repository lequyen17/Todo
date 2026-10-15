package com.example.todo_api.modules.users.entity;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.users.UserErrorCode;

/**
 * Tài khoản người dùng. Trạng thái chỉ đổi qua các hành vi có tên (không có setter),
 * để những thay đổi đi kèm nhau (vd. mật khẩu + passwordChangedAt) không thể bị quên.
 * Hash mật khẩu do service tính (PasswordEncoder), entity chỉ nhận chuỗi hash.
 */
@Entity
@Table(name = "users")
public class User {

    public static final int DISPLAY_NAME_MAX_LENGTH = 50;
    public static final int AVATAR_URL_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String password;

    @Column(name = "display_name", nullable = false, length = DISPLAY_NAME_MAX_LENGTH)
    private String displayName;

    @Column(name = "avatar_url", length = AVATAR_URL_MAX_LENGTH)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Cho JPA. */
    protected User() {
    }

    private User(String email, String displayName, String passwordHash, Instant emailVerifiedAt) {
        this.email = normalizeEmail(email);
        this.displayName = displayName;
        this.password = passwordHash;
        this.emailVerifiedAt = emailVerifiedAt;
    }

    // ---------------------------------------------------------------- Tạo tài khoản

    /** Đăng ký bằng email + mật khẩu, sau khi đã xác minh OTP email. */
    public static User registerWithPassword(String email, String displayName, String passwordHash, Instant now) {
        return new User(email, truncate(displayName.trim()), passwordHash, now);
    }

    /** Tạo từ Google (email đã được Google xác minh), chưa có mật khẩu. Tên trống -> dùng phần trước @ của email. */
    public static User registerWithGoogle(String email, String googleName, Instant now) {
        String name = googleName == null ? "" : googleName.trim();
        if (name.isEmpty()) {
            String normalized = normalizeEmail(email);
            name = normalized.substring(0, normalized.indexOf('@'));
        }
        return new User(email, truncate(name), null, now);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- Trạng thái

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void ensureActive() {
        if (!isActive()) {
            throw new ApiException(UserErrorCode.ACCOUNT_LOCKED);
        }
    }

    public void lock() {
        status = UserStatus.LOCKED;
    }

    public void recordLogin(Instant now) {
        lastLoginAt = now;
    }

    /** Giữ mốc xác minh cũ nếu đã xác minh trước đó. */
    public void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    // ---------------------------------------------------------------- Mật khẩu

    /** Tài khoản tạo bằng Google chưa có mật khẩu cho tới khi đặt qua "quên mật khẩu". */
    public boolean hasPassword() {
        return password != null;
    }

    /** passwordChangedAt làm mọi access token phát hành trước đó mất hiệu lực (xem JwtAuthFilter). */
    public void changePassword(String newPasswordHash, Instant now) {
        password = newPasswordHash;
        passwordChangedAt = now;
    }

    // ---------------------------------------------------------------- Avatar

    public void changeAvatar(String url) {
        if (url != null && url.length() > AVATAR_URL_MAX_LENGTH) {
            throw new ApiException(UserErrorCode.AVATAR_URL_TOO_LONG);
        }
        avatarUrl = url;
    }

    /** Ảnh từ nhà cung cấp đăng nhập (Google): chỉ dùng khi user chưa có avatar, bỏ qua URL quá dài. */
    public void useAvatarIfMissing(String url) {
        if (avatarUrl == null && url != null && url.length() <= AVATAR_URL_MAX_LENGTH) {
            avatarUrl = url;
        }
    }

    private static String truncate(String displayName) {
        return displayName.length() > DISPLAY_NAME_MAX_LENGTH
                ? displayName.substring(0, DISPLAY_NAME_MAX_LENGTH)
                : displayName;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
