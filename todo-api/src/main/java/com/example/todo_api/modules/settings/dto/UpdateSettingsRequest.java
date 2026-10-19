package com.example.todo_api.modules.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.example.todo_api.modules.settings.entity.UserSettings;

/**
 * Gửi đủ bộ cài đặt. Client quy đổi "Nhắc trước" (số + phút / giờ / ngày) ra phút trước khi gửi (VR-12).
 * Quy tắc "bật thì phải có ít nhất một kênh" nằm ở UserSettings.update.
 */
public record UpdateSettingsRequest(
        @NotNull Boolean reminderEnabled,
        @NotNull Boolean channelWeb,
        @NotNull Boolean channelEmail,
        @NotNull(message = "Remind before is required")
        @Min(value = UserSettings.MIN_REMIND_BEFORE_MINUTES, message = "Remind before must be at least 1 minute")
        @Max(value = UserSettings.MAX_REMIND_BEFORE_MINUTES, message = "Remind before must be at most 7 days")
        Integer remindBeforeMinutes) {
}
