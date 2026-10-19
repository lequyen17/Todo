package com.example.todo_api.modules.reminders;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job gửi nhắc nhở (FR-04.2): mỗi 1 phút nhận các reminder tới hạn theo lô rồi gửi từng cái.
 * Lô đầy thì lấy tiếp ngay, để không dồn sang lần chạy sau khi có nhiều reminder cùng lúc.
 */
@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    static final int BATCH_SIZE = 200;

    private final ReminderClaimRepository claimRepository;
    private final ReminderDispatcher dispatcher;

    public ReminderJob(ReminderClaimRepository claimRepository, ReminderDispatcher dispatcher) {
        this.claimRepository = claimRepository;
        this.dispatcher = dispatcher;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void run() {
        List<DueReminder> batch;
        do {
            batch = claimRepository.claimDue(BATCH_SIZE);
            Instant now = Instant.now();
            for (DueReminder reminder : batch) {
                try {
                    dispatcher.dispatch(reminder, now);
                } catch (RuntimeException e) {
                    // Một reminder lỗi không chặn các reminder còn lại; nó đã được đánh dấu nên không gửi lại
                    log.error("Failed to dispatch reminder for task {}", reminder.taskId(), e);
                }
            }
        } while (batch.size() == BATCH_SIZE);
    }
}
