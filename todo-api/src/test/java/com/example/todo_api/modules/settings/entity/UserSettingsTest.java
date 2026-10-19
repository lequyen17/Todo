package com.example.todo_api.modules.settings.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.settings.SettingsErrorCode;

class UserSettingsTest {

    @Test
    void defaults_areOnBothChannelsOneHour() {
        UserSettings settings = UserSettings.defaultsFor(1L);

        assertThat(settings.isReminderEnabled()).isTrue();
        assertThat(settings.isChannelWeb()).isTrue();
        assertThat(settings.isChannelEmail()).isTrue();
        assertThat(settings.getRemindBeforeMinutes()).isEqualTo(60);
    }

    @Test
    void update_enabledWithoutChannel_isRejectedAndUnchanged() {
        UserSettings settings = UserSettings.defaultsFor(1L);

        assertThatThrownBy(() -> settings.update(true, false, false, 120))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(SettingsErrorCode.REMINDER_CHANNEL_REQUIRED);
        assertThat(settings.getRemindBeforeMinutes()).isEqualTo(60);
    }

    @Test
    void update_disabledWithoutChannel_isAllowed() {
        UserSettings settings = UserSettings.defaultsFor(1L);

        settings.update(false, false, false, 30);

        assertThat(settings.isReminderEnabled()).isFalse();
        assertThat(settings.getRemindBeforeMinutes()).isEqualTo(30);
    }

    @Test
    void update_savesAllFields() {
        UserSettings settings = UserSettings.defaultsFor(1L);

        settings.update(true, false, true, 120);

        assertThat(settings.isChannelWeb()).isFalse();
        assertThat(settings.isChannelEmail()).isTrue();
        assertThat(settings.getRemindBeforeMinutes()).isEqualTo(120);
    }
}
