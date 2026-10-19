package com.example.todo_api.modules.reminders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.infrastructure.email.MailService;
import com.example.todo_api.modules.notifications.service.NotificationService;
import com.example.todo_api.modules.settings.entity.UserSettings;
import com.example.todo_api.modules.settings.repository.UserSettingsRepository;
import com.example.todo_api.modules.tasks.repository.TaskRepository;

/**
 * Gửi một reminder đã được nhận xử lý theo cài đặt của người tạo task (FR-04.2 bước 3–6):
 * <ul>
 * <li>Đang tắt nhắc nhở: không gửi gì (E3); reminder đã được đánh dấu đã gửi từ trước.</li>
 * <li>Ngay trước khi gửi ở mỗi kênh, kiểm tra lại task: đã xóa / đã hoàn thành thì thôi (E1).</li>
 * <li>Kênh trong web: lưu thông báo, sự kiện realtime đẩy sau commit (FR-04.3, FR-06).</li>
 * <li>Kênh email: gửi trên reminderExecutor, lỗi thì thử lại 3 lần; vẫn lỗi thì ghi log để xử lý sau (E2).
 * Hai kênh độc lập: email lỗi không ảnh hưởng thông báo trong web (AC4).</li>
 * </ul>
 */
@Component
public class ReminderDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);

    /** Chờ trước mỗi lần thử lại email: 3 lần thử lại sau lần gửi đầu. */
    static final List<Duration> EMAIL_RETRY_BACKOFF = List.of(Duration.ofSeconds(1), Duration.ofSeconds(2),
            Duration.ofSeconds(4));

    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final UserSettingsRepository settingsRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final TransactionTemplate transaction;
    private final Executor reminderExecutor;
    private final String frontendUrl;
    private final Sleeper sleeper;

    @Autowired
    public ReminderDispatcher(UserSettingsRepository settingsRepository, TaskRepository taskRepository,
            NotificationService notificationService, MailService mailService,
            PlatformTransactionManager transactionManager, @Qualifier("reminderExecutor") Executor reminderExecutor,
            AppProperties props) {
        this(settingsRepository, taskRepository, notificationService, mailService,
                new TransactionTemplate(transactionManager), reminderExecutor, props.frontendUrl(),
                duration -> Thread.sleep(duration.toMillis()));
    }

    ReminderDispatcher(UserSettingsRepository settingsRepository, TaskRepository taskRepository,
            NotificationService notificationService, MailService mailService, TransactionTemplate transaction,
            Executor reminderExecutor, String frontendUrl, Sleeper sleeper) {
        this.settingsRepository = settingsRepository;
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.mailService = mailService;
        this.transaction = transaction;
        this.reminderExecutor = reminderExecutor;
        this.frontendUrl = frontendUrl;
        this.sleeper = sleeper;
    }

    public void dispatch(DueReminder reminder, Instant now) {
        UserSettings settings = settingsRepository.findById(reminder.userId()).orElse(null);
        if (settings == null || !settings.isReminderEnabled()) {
            return;
        }
        String message = ReminderMessage.format(reminder.title(), reminder.dueDate(), now);

        if (settings.isChannelWeb()) {
            try {
                createWebNotification(reminder, message);
            } catch (RuntimeException e) {
                log.error("Failed to create reminder notification for task {} (user {})", reminder.taskId(),
                        reminder.userId(), e);
            }
        }
        if (settings.isChannelEmail()) {
            reminderExecutor.execute(() -> sendEmailWithRetry(reminder, message));
        }
    }

    private void createWebNotification(DueReminder reminder, String message) {
        transaction.executeWithoutResult(status -> {
            if (taskRepository.existsByIdAndCompletedFalse(reminder.taskId())) {
                notificationService.createReminder(reminder.userId(), reminder.taskId(), message);
            }
        });
    }

    private void sendEmailWithRetry(DueReminder reminder, String message) {
        String link = frontendUrl + "/inbox?task=" + reminder.taskId();
        for (int attempt = 0; attempt <= EMAIL_RETRY_BACKOFF.size(); attempt++) {
            if (!taskRepository.existsByIdAndCompletedFalse(reminder.taskId())) {
                return;
            }
            try {
                mailService.sendTaskReminder(reminder.email(), reminder.displayName(), message, link);
                return;
            } catch (MailException e) {
                if (attempt == EMAIL_RETRY_BACKOFF.size()) {
                    log.error("Reminder email for task {} to {} failed after {} retries; handle manually",
                            reminder.taskId(), reminder.email(), EMAIL_RETRY_BACKOFF.size(), e);
                    return;
                }
                log.warn("Reminder email for task {} failed (attempt {}), retrying: {}", reminder.taskId(),
                        attempt + 1, e.getMessage());
            }
            try {
                sleeper.sleep(EMAIL_RETRY_BACKOFF.get(attempt));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Reminder email for task {} was interrupted before retrying", reminder.taskId());
                return;
            }
        }
    }
}
