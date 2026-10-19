package com.example.todo_api.modules.reminders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.todo_api.infrastructure.email.MailService;
import com.example.todo_api.modules.notifications.service.NotificationService;
import com.example.todo_api.modules.settings.entity.UserSettings;
import com.example.todo_api.modules.settings.repository.UserSettingsRepository;
import com.example.todo_api.modules.tasks.repository.TaskRepository;

class ReminderDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-10-20T09:00:00Z");
    private static final DueReminder REMINDER = new DueReminder(10L, 7L, "Submit assignment",
            NOW.plus(Duration.ofHours(3)), "huy@example.com", "Huy");
    private static final String MESSAGE = "\"Submit assignment\" is due in 3 hours.";

    private UserSettingsRepository settingsRepository;
    private TaskRepository taskRepository;
    private NotificationService notificationService;
    private MailService mailService;
    private List<Duration> sleeps;
    private ReminderDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(UserSettingsRepository.class);
        taskRepository = mock(TaskRepository.class);
        notificationService = mock(NotificationService.class);
        mailService = mock(MailService.class);
        sleeps = new ArrayList<>();
        when(taskRepository.existsByIdAndCompletedFalse(10L)).thenReturn(true);
        // Executor chạy ngay trên luồng test; sleeper chỉ ghi lại thời gian chờ
        dispatcher = new ReminderDispatcher(settingsRepository, taskRepository, notificationService, mailService,
                new TransactionTemplate(mock(PlatformTransactionManager.class)), Runnable::run,
                "http://localhost", sleeps::add);
    }

    private void settings(boolean enabled, boolean web, boolean email) {
        UserSettings settings = UserSettings.defaultsFor(7L);
        settings.update(enabled, web, email, 60);
        when(settingsRepository.findById(7L)).thenReturn(Optional.of(settings));
    }

    /** FR-04.2 AC1: bật cả hai kênh thì nhận cả thông báo trong web và email MSG-04. */
    @Test
    void bothChannels_createNotificationAndSendEmail() {
        settings(true, true, true);

        dispatcher.dispatch(REMINDER, NOW);

        verify(notificationService).createReminder(7L, 10L, MESSAGE);
        verify(mailService).sendTaskReminder("huy@example.com", "Huy", MESSAGE, "http://localhost/inbox?task=10");
    }

    /** E3 / AC5: đang tắt nhắc nhở thì không có thông báo hay email nào. */
    @Test
    void remindersOff_sendsNothing() {
        settings(false, true, true);

        dispatcher.dispatch(REMINDER, NOW);

        verifyNoInteractions(notificationService, mailService);
    }

    @Test
    void onlyEnabledChannelsAreUsed() {
        settings(true, false, true);

        dispatcher.dispatch(REMINDER, NOW);

        verify(notificationService, never()).createReminder(anyLong(), anyLong(), anyString());
        verify(mailService).sendTaskReminder(anyString(), anyString(), anyString(), anyString());
    }

    /** E1 / AC2: task đã hoàn thành hoặc bị xóa trước khi gửi thì không gửi. */
    @Test
    void taskCompletedOrDeleted_sendsNothing() {
        settings(true, true, true);
        when(taskRepository.existsByIdAndCompletedFalse(10L)).thenReturn(false);

        dispatcher.dispatch(REMINDER, NOW);

        verifyNoInteractions(notificationService, mailService);
    }

    /** E2 / AC4: email lỗi cả 3 lần thử lại; thông báo trong web vẫn được tạo. */
    @Test
    void emailFailingEveryTime_isRetriedThreeTimes_webNotificationStillCreated() {
        settings(true, true, true);
        doThrow(new MailSendException("SMTP down")).when(mailService)
                .sendTaskReminder(anyString(), anyString(), anyString(), anyString());

        dispatcher.dispatch(REMINDER, NOW);

        verify(mailService, times(4)).sendTaskReminder(anyString(), anyString(), anyString(), anyString());
        assertThat(sleeps).containsExactlyElementsOf(ReminderDispatcher.EMAIL_RETRY_BACKOFF);
        verify(notificationService).createReminder(7L, 10L, MESSAGE);
    }

    @Test
    void emailSucceedingOnRetry_stopsRetrying() {
        settings(true, false, true);
        doThrow(new MailSendException("SMTP down")).doNothing().when(mailService)
                .sendTaskReminder(anyString(), anyString(), anyString(), anyString());

        dispatcher.dispatch(REMINDER, NOW);

        verify(mailService, times(2)).sendTaskReminder(anyString(), anyString(), anyString(), anyString());
        assertThat(sleeps).containsExactly(Duration.ofSeconds(1));
    }

    /** Lỗi khi lưu thông báo không làm mất email. */
    @Test
    void webNotificationFailure_doesNotBlockEmail() {
        settings(true, true, true);
        when(notificationService.createReminder(anyLong(), anyLong(), anyString()))
                .thenThrow(new IllegalStateException("db down"));

        dispatcher.dispatch(REMINDER, NOW);

        verify(mailService).sendTaskReminder(eq("huy@example.com"), any(), any(), any());
    }
}
