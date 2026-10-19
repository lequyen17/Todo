package com.example.todo_api.modules.tasks.dto;

import jakarta.validation.constraints.NotNull;

/** Tick / bỏ tick checkbox (FR-02.4). */
public record CompletionRequest(@NotNull(message = "Completed is required") Boolean completed) {
}
