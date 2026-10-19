package com.example.todo_api.modules.tasks.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import com.example.todo_api.modules.tasks.entity.Priority;
import com.example.todo_api.modules.tasks.search.DueFilter;
import com.example.todo_api.modules.tasks.search.TaskSort;

/**
 * Tham số query của GET /api/tasks (FR-02.2, FR-03). Không gửi thì lấy mặc định:
 * ẩn task đã hoàn thành, sắp theo due date tăng dần, trang 0, 20 task (FR-02.2, FR-03.3).
 * <ul>
 * <li>q: từ khóa; rỗng hoặc chỉ khoảng trắng = không tìm (FR-03.1 AC3).</li>
 * <li>showCompleted: true thì hiện cả task đã hoàn thành (nút "Show completed", FR-02.2).</li>
 * <li>priority / due: không gửi = All.</li>
 * <li>direction: không gửi thì theo tiêu chí: due date tăng dần, priority HIGH → LOW,
 * created date mới nhất trước.</li>
 * <li>tz: múi giờ IANA của trình duyệt, để tính "hôm nay" cho bộ lọc Due (BR-10).</li>
 * </ul>
 */
public record TaskListParams(
        @Size(max = 200, message = "Search keyword must be at most 200 characters") String q,
        Boolean showCompleted,
        Priority priority,
        DueFilter due,
        TaskSort sort,
        Direction direction,
        String tz,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size) {

    public enum Direction {
        ASC, DESC
    }

    public static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";

    public TaskListParams {
        q = q == null || q.isBlank() ? null : q.strip();
        showCompleted = Boolean.TRUE.equals(showCompleted);
        sort = sort == null ? TaskSort.DUE_DATE : sort;
        direction = direction == null ? defaultDirection(sort) : direction;
        tz = tz == null || tz.isBlank() ? DEFAULT_TIME_ZONE : tz;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    private static Direction defaultDirection(TaskSort sort) {
        return switch (sort) {
            case DUE_DATE -> Direction.ASC;
            case PRIORITY, CREATED_AT -> Direction.DESC;
        };
    }
}
