package com.example.todo_api.modules.tasks.search;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Bộ lọc Due (FR-03.2). "Hôm nay" tính theo múi giờ của trình duyệt (BR-10), nên khoảng thời gian phụ thuộc zone.
 * TODAY / OVERDUE / UPCOMING chỉ gồm task chưa hoàn thành (BR-07, BR-08, BR-09).
 */
public enum DueFilter {
    TODAY, OVERDUE, UPCOMING, NO_DUE_DATE;

    /** Khoảng [from, to) của due date; null nghĩa là không chặn đầu đó. */
    public record Range(Instant from, Instant to) {
    }

    public Range range(Instant now, ZoneId zone) {
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        return switch (this) {
            // BR-07: dueDate < now
            case OVERDUE -> new Range(null, now);
            // BR-08: trong ngày hôm nay
            case TODAY -> new Range(startOfToday, startOfTomorrow);
            // BR-09: 7 ngày tới, không tính hôm nay
            case UPCOMING -> new Range(startOfTomorrow, today.plusDays(8).atStartOfDay(zone).toInstant());
            case NO_DUE_DATE -> null;
        };
    }
}
