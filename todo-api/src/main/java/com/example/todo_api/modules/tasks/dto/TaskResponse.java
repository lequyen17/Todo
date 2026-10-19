package com.example.todo_api.modules.tasks.dto;

import java.time.Instant;

import com.example.todo_api.modules.tasks.entity.Priority;
import com.example.todo_api.modules.tasks.entity.Task;

/** overdue tính sẵn theo BR-07 để UI tô đỏ due date. */
public record TaskResponse(
        Long id,
        String title,
        String description,
        Priority priority,
        boolean completed,
        Instant dueDate,
        boolean dueHasTime,
        Instant reminderAt,
        boolean reminderSent,
        boolean overdue,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task, Instant now) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.isCompleted(),
                task.getDueDate(),
                task.isDueHasTime(),
                task.getReminderAt(),
                task.isReminderSent(),
                task.isOverdue(now),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
