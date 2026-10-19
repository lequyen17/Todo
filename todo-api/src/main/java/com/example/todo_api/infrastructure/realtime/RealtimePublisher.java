package com.example.todo_api.infrastructure.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Đẩy sự kiện tới mọi kết nối đang mở của một user. Sự kiện chỉ cho biết loại thay đổi và id, không mang dữ liệu:
 * trình duyệt tự gọi REST để lấy dữ liệu mới, quyền truy cập vẫn được kiểm tra như mọi lần (FR-06.2).
 */
@Component
public class RealtimePublisher {

    static final String USER_EVENTS_DESTINATION = "/queue/events";

    public record Event(String type, Long id) {
    }

    private final SimpMessagingTemplate messaging;

    public RealtimePublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void sendToUser(Long userId, Event event) {
        messaging.convertAndSendToUser(String.valueOf(userId), USER_EVENTS_DESTINATION, event);
    }
}
