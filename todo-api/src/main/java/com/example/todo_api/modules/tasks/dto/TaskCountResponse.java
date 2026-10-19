package com.example.todo_api.modules.tasks.dto;

/** Số task chưa hoàn thành, hiển thị cạnh Inbox trên thanh điều hướng (FR-02.0). */
public record TaskCountResponse(long incomplete) {
}
