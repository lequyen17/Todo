package com.example.todo_api.modules.users.dto;

import java.time.Instant;

import com.example.todo_api.modules.users.entity.Role;
import com.example.todo_api.modules.users.entity.User;

public record UserProfileResponse(
        Long id,
        String email,
        String displayName,
        String avatarUrl,
        Role role,
        Instant emailVerifiedAt,
        Instant lastLoginAt,
        Instant createdAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
