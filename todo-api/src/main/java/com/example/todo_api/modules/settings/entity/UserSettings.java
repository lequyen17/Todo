package com.example.todo_api.modules.settings.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.settings.SettingsErrorCode;

/** Cài đặt nhắc nhở; mỗi user đúng một bộ, tạo cùng lúc với tài khoản (FR-01.1, FR-05.2). */
@Entity
@Table(name = "user_settings")
public class UserSettings {

    public static final int DEFAULT_REMIND_BEFORE_MINUTES = 60;
    /** VR-12: "Nhắc trước" quy ra phút nằm trong 1 phút .. 7 ngày. */
    public static final int MIN_REMIND_BEFORE_MINUTES = 1;
    public static final int MAX_REMIND_BEFORE_MINUTES = 7 * 24 * 60;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled = true;

    @Column(name = "channel_web", nullable = false)
    private boolean channelWeb = true;

    @Column(name = "channel_email", nullable = false)
    private boolean channelEmail = true;

    /** "Nhắc trước": client dùng để điền sẵn reminder trên form tạo task (FR-04.1). */
    @Column(name = "remind_before_minutes", nullable = false)
    private int remindBeforeMinutes = DEFAULT_REMIND_BEFORE_MINUTES;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Cho JPA. */
    protected UserSettings() {
    }

    private UserSettings(Long userId) {
        this.userId = userId;
    }

    /** Mặc định: bật nhắc nhở, cả hai kênh, nhắc trước 1 giờ (FR-05.2 AC1). */
    public static UserSettings defaultsFor(Long userId) {
        return new UserSettings(userId);
    }

    /**
     * Lưu cả bộ cài đặt (FR-05.2). Đổi "Nhắc trước" không chạm reminder của task đã có (AC3);
     * tắt nhắc nhở không xóa lựa chọn kênh, bật lại thì dùng tiếp.
     */
    public void update(boolean reminderEnabled, boolean channelWeb, boolean channelEmail, int remindBeforeMinutes) {
        if (reminderEnabled && !channelWeb && !channelEmail) {
            throw new ApiException(SettingsErrorCode.REMINDER_CHANNEL_REQUIRED);
        }
        this.reminderEnabled = reminderEnabled;
        this.channelWeb = channelWeb;
        this.channelEmail = channelEmail;
        this.remindBeforeMinutes = remindBeforeMinutes;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public boolean isChannelWeb() {
        return channelWeb;
    }

    public boolean isChannelEmail() {
        return channelEmail;
    }

    public int getRemindBeforeMinutes() {
        return remindBeforeMinutes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
