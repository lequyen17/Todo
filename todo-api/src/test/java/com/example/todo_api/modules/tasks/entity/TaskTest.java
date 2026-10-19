package com.example.todo_api.modules.tasks.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.tasks.TaskErrorCode;

class TaskTest {

    private static final Instant NOW = Instant.parse("2026-10-19T03:00:00Z");
    private static final Instant TOMORROW_17H = Instant.parse("2026-10-20T10:00:00Z");
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    // ---------------------------------------------------------------- Tạo (FR-02.1, FR-04.1)

    @Test
    void create_withTitleOnly_usesDefaults() {
        Task task = Task.create(1L, "Buy milk", null, null, null, false, null, NOW);

        assertThat(task.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(task.isCompleted()).isFalse();
        assertThat(task.getReminderAt()).isNull();
        assertThat(task.isDueHasTime()).isFalse();
    }

    @Test
    void create_keepsReminderChosenOnForm() {
        Instant reminder = TOMORROW_17H.minus(Duration.ofHours(2));

        Task task = Task.create(1L, "Report", null, Priority.HIGH, TOMORROW_17H, true, reminder, NOW);

        assertThat(task.getReminderAt()).isEqualTo(reminder);
        assertThat(task.isReminderSent()).isFalse();
    }

    @Test
    void create_userClearedReminder_hasNoReminder() {
        Task task = Task.create(1L, "Report", null, null, TOMORROW_17H, true, null, NOW);

        assertThat(task.getDueDate()).isEqualTo(TOMORROW_17H);
        assertThat(task.getReminderAt()).isNull();
    }

    @Test
    void create_appliesReminderRules() {
        assertRejected(() -> Task.create(1L, "T", null, null, null, false, TOMORROW_17H, NOW),
                TaskErrorCode.REMINDER_REQUIRES_DUE_DATE);
        assertRejected(() -> Task.create(1L, "T", null, null, TOMORROW_17H, true, NOW.minusSeconds(60), NOW),
                TaskErrorCode.REMINDER_IN_PAST);
        assertRejected(() -> Task.create(1L, "T", null, null, TOMORROW_17H, true, TOMORROW_17H.plusSeconds(60), NOW),
                TaskErrorCode.REMINDER_AFTER_DUE_DATE);
    }

    @Test
    void create_withoutDueDate_ignoresDueHasTime() {
        Task task = Task.create(1L, "Read", null, null, null, true, null, NOW);

        assertThat(task.isDueHasTime()).isFalse();
    }

    // ---------------------------------------------------------------- Sửa (FR-02.3, FR-04.1)

    @Test
    void update_doesNotTouchCompletion() {
        Task task = taskDueTomorrow();
        task.setCompleted(true, NOW);

        task.update("New title", null, Priority.LOW, TOMORROW_17H, true, task.getReminderAt(), NOW.plusSeconds(60));

        assertThat(task.getTitle()).isEqualTo("New title");
        assertThat(task.isCompleted()).isTrue();
        assertThat(task.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void update_reminderWithoutDueDate_isRejected() {
        Task task = Task.create(1L, "No due", null, null, null, false, null, NOW);

        assertRejected(() -> task.update("No due", null, Priority.MEDIUM, null, false, TOMORROW_17H, NOW),
                TaskErrorCode.REMINDER_REQUIRES_DUE_DATE);
    }

    @Test
    void update_removingDueDate_clearsReminder() {
        Task task = taskDueTomorrow();

        task.update("Report", null, Priority.MEDIUM, null, false, task.getReminderAt(), NOW);

        assertThat(task.getDueDate()).isNull();
        assertThat(task.getReminderAt()).isNull();
    }

    @Test
    void update_dueDateEarlierThanReminder_isRejectedAndTaskUnchanged() {
        Task task = taskDueTomorrow();
        Instant reminder = task.getReminderAt();
        Instant earlierDue = reminder.minus(ONE_HOUR);

        assertRejected(() -> task.update("Changed", null, Priority.MEDIUM, earlierDue, true, reminder, NOW),
                TaskErrorCode.REMINDER_AFTER_DUE_DATE);
        assertThat(task.getTitle()).isEqualTo("Report");
        assertThat(task.getDueDate()).isEqualTo(TOMORROW_17H);
    }

    @Test
    void update_newReminderInThePast_isRejected() {
        Task task = taskDueTomorrow();

        assertRejected(() -> task.update("Report", null, Priority.MEDIUM, TOMORROW_17H, true,
                NOW.minusSeconds(60), NOW), TaskErrorCode.REMINDER_IN_PAST);
    }

    @Test
    void update_keepingAPastReminder_isAllowed() {
        Task task = taskDueTomorrow();
        Instant reminder = task.getReminderAt();
        Instant afterReminder = reminder.plusSeconds(60);

        task.update("Renamed", null, Priority.MEDIUM, TOMORROW_17H, true, reminder, afterReminder);

        assertThat(task.getTitle()).isEqualTo("Renamed");
        assertThat(task.getReminderAt()).isEqualTo(reminder);
    }

    @Test
    void update_reminderEqualToDueDate_isAllowed() {
        Task task = taskDueTomorrow();

        task.update("Report", null, Priority.MEDIUM, TOMORROW_17H, true, TOMORROW_17H, NOW);

        assertThat(task.getReminderAt()).isEqualTo(TOMORROW_17H);
    }

    // ---------------------------------------------------------------- Hoàn thành (FR-02.4, BR-12)

    @Test
    void setCompleted_tickAndUntick() {
        Task task = taskDueTomorrow();

        task.setCompleted(true, NOW);
        assertThat(task.isCompleted()).isTrue();
        assertThat(task.getCompletedAt()).isEqualTo(NOW);

        task.setCompleted(false, NOW.plusSeconds(60));
        assertThat(task.isCompleted()).isFalse();
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void setCompleted_sameStateTwice_keepsOriginalCompletedAt() {
        Task task = taskDueTomorrow();
        task.setCompleted(true, NOW);

        task.setCompleted(true, NOW.plusSeconds(60));

        assertThat(task.getCompletedAt()).isEqualTo(NOW);
    }

    // ---------------------------------------------------------------- Overdue (BR-07)

    @Test
    void isOverdue_onlyWhenIncompleteAndPastDue() {
        Task task = taskDueTomorrow();
        Instant afterDue = TOMORROW_17H.plusSeconds(1);

        assertThat(task.isOverdue(NOW)).isFalse();
        assertThat(task.isOverdue(afterDue)).isTrue();

        task.setCompleted(true, afterDue);
        assertThat(task.isOverdue(afterDue)).isFalse();
    }

    private static Task taskDueTomorrow() {
        return Task.create(1L, "Report", null, null, TOMORROW_17H, true, TOMORROW_17H.minus(ONE_HOUR), NOW);
    }

    private static void assertRejected(ThrowingCallable call, TaskErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(code);
    }
}
