package com.example.todo_api.modules.tasks.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.todo_api.modules.tasks.entity.Priority;
import com.example.todo_api.modules.tasks.entity.Task;

/**
 * Gửi đủ form sửa (FR-02.3): trường để null nghĩa là xóa giá trị đó.
 * Quy tắc reminder / due date nằm ở Task.update.
 */
public record UpdateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = Task.TITLE_MAX_LENGTH, message = "Title must be at most 200 characters")
        String title,
        @Size(max = Task.DESCRIPTION_MAX_LENGTH, message = "Description must be at most 2000 characters")
        String description,
        @NotNull(message = "Priority is required")
        Priority priority,
        Instant dueDate,
        boolean dueHasTime,
        Instant reminderAt) {

    public UpdateTaskRequest {
        title = TaskText.trimTitle(title);
        description = TaskText.blankToNull(description);
    }
}
