package com.example.todo_api.modules.notifications;

/** Sự kiện trong ứng dụng: vừa lưu một thông báo; đẩy realtime tới người nhận sau khi commit (FR-06.2). */
public record NotificationCreated(Long userId, Long notificationId) {
}
