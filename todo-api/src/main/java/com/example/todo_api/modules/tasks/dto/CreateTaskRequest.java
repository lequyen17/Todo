package com.example.todo_api.modules.tasks.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.todo_api.modules.tasks.entity.Priority;
import com.example.todo_api.modules.tasks.entity.Task;

/**
 * Tạo nhanh chỉ cần title (FR-02.1).
 * reminderAt: form điền sẵn dueDate − "Nhắc trước" (GET /api/settings), bỏ trống nếu giá trị đó đã qua;
 * user không sửa thì gửi nguyên giá trị điền sẵn, user xóa thì gửi null.
 * dueHasTime = false: user chỉ chọn ngày, client gửi dueDate là 23:59 giờ local đã quy ra UTC (BR-11).
 */
public record CreateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = Task.TITLE_MAX_LENGTH, message = "Title must be at most 200 characters")
        String title,
        @Size(max = Task.DESCRIPTION_MAX_LENGTH, message = "Description must be at most 2000 characters")
        String description,
        Priority priority,
        Instant dueDate,
        boolean dueHasTime,
        Instant reminderAt) {

    public CreateTaskRequest {
        title = TaskText.trimTitle(title);
        description = TaskText.blankToNull(description);
    }
}
