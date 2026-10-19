package com.example.todo_api.modules.reminders;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class ReminderMessageTest {

    private static final Instant NOW = Instant.parse("2026-10-20T09:00:00Z");

    private static String dueIn(Duration remaining) {
        return ReminderMessage.format("Submit assignment", NOW.plus(remaining), NOW);
    }

    /** Ví dụ trong MSG-04. */
    @Test
    void hours() {
        assertThat(dueIn(Duration.ofHours(3))).isEqualTo("\"Submit assignment\" is due in 3 hours.");
        assertThat(dueIn(Duration.ofMinutes(60))).isEqualTo("\"Submit assignment\" is due in 1 hour.");
    }

    @Test
    void minutes_underAnHour() {
        assertThat(dueIn(Duration.ofMinutes(45))).isEqualTo("\"Submit assignment\" is due in 45 minutes.");
        assertThat(dueIn(Duration.ofSeconds(30))).isEqualTo("\"Submit assignment\" is due in 1 minute.");
    }

    /** Job chạy trễ vài giây so với giờ nhắc: 1 giờ trước hạn vẫn ra "1 hour", không phải "59 minutes". */
    @Test
    void reminderSentSlightlyLate_stillRoundsToTheHour() {
        assertThat(dueIn(Duration.ofMinutes(59).plusSeconds(20))).isEqualTo("\"Submit assignment\" is due in 1 hour.");
    }

    @Test
    void days() {
        assertThat(dueIn(Duration.ofDays(2))).isEqualTo("\"Submit assignment\" is due in 2 days.");
        assertThat(dueIn(Duration.ofHours(24))).isEqualTo("\"Submit assignment\" is due in 1 day.");
    }

    /** Reminder đặt đúng bằng due date (VR-10 cho phép reminderAt = dueDate). */
    @Test
    void atOrPastDueDate() {
        assertThat(dueIn(Duration.ZERO)).isEqualTo("\"Submit assignment\" is due now.");
        assertThat(dueIn(Duration.ofMinutes(-2))).isEqualTo("\"Submit assignment\" is due now.");
    }
}
