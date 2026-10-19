package com.example.todo_api.modules.notifications.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Thông báo trong web (FR-04.3). Loại duy nhất là nhắc nhở task, phát sinh từ FR-04.2.
 * message là MSG-04 chốt tại lúc gửi ("... is due in 3 hours"), không tính lại khi xem.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    public static final int MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;

    @Column(nullable = false, length = MESSAGE_MAX_LENGTH, updatable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Cho JPA. */
    protected Notification() {
    }

    private Notification(Long userId, Long taskId, String message) {
        this.userId = userId;
        this.taskId = taskId;
        this.message = message;
    }

    public static Notification reminder(Long userId, Long taskId, String message) {
        return new Notification(userId, taskId, message);
    }

    public void markRead() {
        read = true;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
