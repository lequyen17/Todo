package com.example.todo_api.modules.notifications.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.todo_api.infrastructure.realtime.RealtimePublisher;
import com.example.todo_api.modules.notifications.NotificationCreated;

/** Báo cho người nhận có thông báo mới, chỉ sau khi thông báo đã lưu thành công (FR-06.2). */
@Component
public class NotificationRealtimePublisher {

    static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";

    private final RealtimePublisher realtime;

    public NotificationRealtimePublisher(RealtimePublisher realtime) {
        this.realtime = realtime;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onCreated(NotificationCreated event) {
        realtime.sendToUser(event.userId(), new RealtimePublisher.Event(NOTIFICATION_CREATED, event.notificationId()));
    }
}
