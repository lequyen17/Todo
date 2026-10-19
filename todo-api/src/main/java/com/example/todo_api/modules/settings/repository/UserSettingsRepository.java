package com.example.todo_api.modules.settings.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.todo_api.modules.settings.entity.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
}
