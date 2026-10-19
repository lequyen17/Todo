package com.example.todo_api.modules.tasks.dto;

/** Chuẩn hóa text của task trước khi validate. */
final class TaskText {

    private TaskText() {
    }

    /** Bỏ khoảng trắng đầu/cuối, để title chỉ gồm khoảng trắng bị @NotBlank chặn và giới hạn 200 tính trên phần chữ (VR-06). */
    static String trimTitle(String title) {
        return title == null ? null : title.strip();
    }

    /** Description chỉ gồm khoảng trắng coi như không có. */
    static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
