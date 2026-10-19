package com.example.todo_api.modules.settings.dto;

import com.example.todo_api.modules.settings.entity.UserSettings;

public record SettingsResponse(
        boolean reminderEnabled,
        boolean channelWeb,
        boolean channelEmail,
        int remindBeforeMinutes) {

    public static SettingsResponse from(UserSettings settings) {
        return new SettingsResponse(
                settings.isReminderEnabled(),
                settings.isChannelWeb(),
                settings.isChannelEmail(),
                settings.getRemindBeforeMinutes());
    }
}
