package com.example.todo_api.modules.auth.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.example.todo_api.modules.users.entity.User;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "replaced_by_id")
    private Long replacedById;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    private RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt) {
        this.user = user;
        this.token = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
    }

    /**
     * Token mới trong family: hạn = now + ttl nhưng không vượt quá sessionEnd (hạn tối đa của cả phiên).
     * Chỉ lưu hash; token gốc chỉ gửi cho client.
     */
    public static RefreshToken issue(User user, String tokenHash, UUID familyId, Instant now, Duration ttl,
            Instant sessionEnd) {
        Instant expiresAt = now.plus(ttl);
        if (expiresAt.isAfter(sessionEnd)) {
            expiresAt = sessionEnd;
        }
        return new RefreshToken(user, tokenHash, familyId, expiresAt);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    /** Đã bị dùng để rotate hoặc đã bị thu hồi -> dùng lại là dấu hiệu token bị đánh cắp. */
    public boolean isUsedOrRevoked() {
        return revokedAt != null || replacedById != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    /** Đã đổi sang token kế tiếp: đánh dấu cả replacedBy và revokedAt để lần dùng lại bị phát hiện. */
    public void rotateTo(RefreshToken next, Instant now) {
        replacedById = next.getId();
        revokedAt = now;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Long getReplacedById() {
        return replacedById;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
