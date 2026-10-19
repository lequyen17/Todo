package com.example.todo_api.modules.reminders;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Nhận xử lý các reminder tới hạn (FR-04.2 bước 1–2): chọn và đánh dấu "đã gửi" trong cùng một câu SQL,
 * nên mỗi reminder chỉ được xử lý đúng một lần (AC3), kể cả khi chạy nhiều instance API (SKIP LOCKED).
 * Không lọc theo cài đặt nhắc nhở: reminder tới hạn lúc user đang tắt vẫn bị đánh dấu, nên bật lại
 * không bị gửi dồn (FR-05.2 AC4). Dùng index idx_tasks_pending_reminder.
 */
@Repository
public class ReminderClaimRepository {

    private static final String CLAIM_DUE = """
            WITH due AS (
                SELECT id FROM tasks
                WHERE reminder_sent = FALSE AND is_completed = FALSE
                  AND reminder_at IS NOT NULL AND reminder_at <= now()
                ORDER BY reminder_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE tasks t SET reminder_sent = TRUE
            FROM due, users u
            WHERE t.id = due.id AND u.id = t.user_id
            RETURNING t.id, t.user_id, t.title, t.due_date, u.email, u.display_name
            """;

    private final JdbcTemplate jdbc;

    public ReminderClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Câu lệnh tự commit (autocommit), nên trả về là việc đánh dấu đã được lưu. */
    public List<DueReminder> claimDue(int limit) {
        return jdbc.query(CLAIM_DUE, (rs, i) -> new DueReminder(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("title"),
                rs.getObject("due_date", OffsetDateTime.class).toInstant(),
                rs.getString("email"),
                rs.getString("display_name")), limit);
    }
}
