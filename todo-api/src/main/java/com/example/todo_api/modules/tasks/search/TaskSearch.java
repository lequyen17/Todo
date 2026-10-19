package com.example.todo_api.modules.tasks.search;

import java.time.ZoneId;

import com.example.todo_api.modules.tasks.entity.Priority;

/**
 * Điều kiện tìm / lọc / sắp xếp task của một user (FR-03). Các bộ lọc kết hợp với nhau và với từ khóa.
 * keyword null = không tìm; showCompleted false = ẩn task đã hoàn thành; priority / due null = All.
 * ascending với PRIORITY: LOW → HIGH.
 */
public record TaskSearch(
        Long userId,
        String keyword,
        boolean showCompleted,
        Priority priority,
        DueFilter due,
        TaskSort sort,
        boolean ascending,
        ZoneId zone) {
}
