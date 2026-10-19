package com.example.todo_api.modules.tasks.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DueFilterTest {

    private static final ZoneId HCM = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 01:30 ngày 20/10 giờ Việt Nam, nhưng vẫn là 19/10 theo UTC. */
    private static final Instant NOW = Instant.parse("2026-10-19T18:30:00Z");

    @Test
    void today_isTheLocalCalendarDay() {
        DueFilter.Range range = DueFilter.TODAY.range(NOW, HCM);

        assertThat(range.from()).isEqualTo(Instant.parse("2026-10-19T17:00:00Z")); // 00:00 20/10 giờ VN
        assertThat(range.to()).isEqualTo(Instant.parse("2026-10-20T17:00:00Z"));   // 00:00 21/10 giờ VN
    }

    @Test
    void upcoming_isTheNextSevenDaysExcludingToday() {
        DueFilter.Range range = DueFilter.UPCOMING.range(NOW, HCM);

        assertThat(range.from()).isEqualTo(Instant.parse("2026-10-20T17:00:00Z")); // 00:00 21/10
        assertThat(range.to()).isEqualTo(Instant.parse("2026-10-27T17:00:00Z"));   // 00:00 28/10, hết ngày 27/10
    }

    @Test
    void overdue_isEverythingBeforeNow() {
        DueFilter.Range range = DueFilter.OVERDUE.range(NOW, HCM);

        assertThat(range.from()).isNull();
        assertThat(range.to()).isEqualTo(NOW);
    }

    @Test
    void today_dependsOnTimeZone() {
        DueFilter.Range utc = DueFilter.TODAY.range(NOW, ZoneId.of("UTC"));

        assertThat(utc.from()).isEqualTo(Instant.parse("2026-10-19T00:00:00Z"));
    }
}
