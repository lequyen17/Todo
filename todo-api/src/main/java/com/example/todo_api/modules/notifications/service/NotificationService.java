package com.example.todo_api.modules.notifications.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.notifications.NotificationCreated;
import com.example.todo_api.modules.notifications.NotificationErrorCode;
import com.example.todo_api.modules.notifications.entity.Notification;
import com.example.todo_api.modules.notifications.repository.NotificationRepository;

/** Thông báo trong web (FR-04.3). User chỉ xem / đánh dấu thông báo của chính mình (BR-03). */
@Service
public class NotificationService {

    /** Mới nhất trước; id phá hòa để thứ tự cố định giữa các trang. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository notificationRepository, ApplicationEventPublisher events) {
        this.notificationRepository = notificationRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(Long userId, int page, int size) {
        return notificationRepository.findByUserId(userId, PageRequest.of(page, size, NEWEST_FIRST));
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public Notification markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead();
        return notification;
    }

    /** "Mark all as read": chuông về 0 (FR-04.3 AC4). */
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    /**
     * Lưu thông báo nhắc nhở (kênh trong web của FR-04.2). Sự kiện realtime chỉ được đẩy sau khi transaction
     * commit (NotificationRealtimePublisher), để trình duyệt lấy lại dữ liệu thì chắc chắn đã thấy thông báo.
     */
    @Transactional
    public Notification createReminder(Long userId, Long taskId, String message) {
        Notification notification = notificationRepository.save(Notification.reminder(userId, taskId, message));
        events.publishEvent(new NotificationCreated(userId, notification.getId()));
        return notification;
    }
}
