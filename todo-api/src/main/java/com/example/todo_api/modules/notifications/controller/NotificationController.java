package com.example.todo_api.modules.notifications.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.core.web.PageResponse;
import com.example.todo_api.modules.notifications.dto.NotificationResponse;
import com.example.todo_api.modules.notifications.dto.UnreadCountResponse;
import com.example.todo_api.modules.notifications.service.NotificationService;

/** Thông báo trong web qua biểu tượng chuông (FR-04.3). */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Mới nhất trước. */
    @GetMapping
    PageResponse<NotificationResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(notificationService.list(principal.userId(), page, size),
                NotificationResponse::from);
    }

    /** Số trên chuông; client gọi khi mở trang và mỗi lần kết nối realtime lại (FR-06.1 AC2). */
    @GetMapping("/unread-count")
    UnreadCountResponse unreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        return new UnreadCountResponse(notificationService.countUnread(principal.userId()));
    }

    @PatchMapping("/{id}/read")
    NotificationResponse markRead(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        return NotificationResponse.from(notificationService.markRead(principal.userId(), id));
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markAllRead(@AuthenticationPrincipal AuthPrincipal principal) {
        notificationService.markAllRead(principal.userId());
    }
}
