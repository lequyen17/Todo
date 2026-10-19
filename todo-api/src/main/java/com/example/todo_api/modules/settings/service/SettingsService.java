package com.example.todo_api.modules.settings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.modules.settings.dto.UpdateSettingsRequest;
import com.example.todo_api.modules.settings.entity.UserSettings;
import com.example.todo_api.modules.settings.repository.UserSettingsRepository;

/** Cài đặt nhắc nhở (FR-05.2). User chỉ đọc / sửa bộ cài đặt của chính mình (BR-03). */
@Service
public class SettingsService {

    private final UserSettingsRepository settingsRepository;

    public SettingsService(UserSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Transactional(readOnly = true)
    public UserSettings get(Long userId) {
        return find(userId);
    }

    @Transactional
    public UserSettings update(Long userId, UpdateSettingsRequest req) {
        UserSettings settings = find(userId);
        settings.update(req.reminderEnabled(), req.channelWeb(), req.channelEmail(), req.remindBeforeMinutes());
        return settings;
    }

    /** Bộ cài đặt được tạo cùng tài khoản (FR-01.1), nên thiếu là lỗi dữ liệu chứ không phải lỗi của user. */
    private UserSettings find(Long userId) {
        return settingsRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User " + userId + " has no settings row"));
    }
}
