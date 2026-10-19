package com.example.todo_api.modules.reminders;

import java.time.Instant;

/** Một reminder tới hạn đã được nhận xử lý (đã đánh dấu reminder_sent), kèm thông tin người tạo task để gửi. */
public record DueReminder(Long taskId, Long userId, String title, Instant dueDate, String email, String displayName) {
}
