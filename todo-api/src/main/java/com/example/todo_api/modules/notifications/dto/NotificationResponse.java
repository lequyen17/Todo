package com.example.todo_api.modules.notifications.dto;

import java.time.Instant;

import com.example.todo_api.modules.notifications.entity.Notification;

public record NotificationResponse(Long id, Long taskId, String message, boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTaskId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
