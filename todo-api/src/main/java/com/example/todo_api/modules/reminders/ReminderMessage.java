package com.example.todo_api.modules.reminders;

import java.time.Duration;
import java.time.Instant;

/** MSG-04: "{title}" is due in {khoảng thời gian còn lại}, vd. "Submit assignment" is due in 3 hours. */
public final class ReminderMessage {

    private ReminderMessage() {
    }

    /** Thời gian còn lại tính tại lúc gửi, làm tròn theo đơn vị lớn nhất: phút (< 1 giờ), giờ (< 1 ngày), ngày. */
    public static String format(String title, Instant dueDate, Instant now) {
        long minutes = (long) Math.ceil(Duration.between(now, dueDate).toSeconds() / 60.0);
        if (minutes <= 0) {
            return "\"%s\" is due now.".formatted(title);
        }
        return "\"%s\" is due in %s.".formatted(title, remaining(minutes));
    }

    private static String remaining(long minutes) {
        if (minutes < 60) {
            return plural(minutes, "minute");
        }
        long hours = Math.round(minutes / 60.0);
        if (hours < 24) {
            return plural(hours, "hour");
        }
        return plural(Math.round(hours / 24.0), "day");
    }

    private static String plural(long n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }
}
