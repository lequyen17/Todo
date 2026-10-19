package com.example.todo_api.modules.settings.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.modules.settings.dto.SettingsResponse;
import com.example.todo_api.modules.settings.dto.UpdateSettingsRequest;
import com.example.todo_api.modules.settings.service.SettingsService;

/** Cài đặt nhắc nhở của chính user (BR-03). */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    SettingsResponse get(@AuthenticationPrincipal AuthPrincipal principal) {
        return SettingsResponse.from(settingsService.get(principal.userId()));
    }

    @PutMapping
    SettingsResponse update(@AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateSettingsRequest req) {
        return SettingsResponse.from(settingsService.update(principal.userId(), req));
    }
}
