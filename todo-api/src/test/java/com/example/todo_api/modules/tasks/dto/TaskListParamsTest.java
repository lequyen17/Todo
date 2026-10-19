package com.example.todo_api.modules.tasks.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.todo_api.modules.tasks.dto.TaskListParams.Direction;
import com.example.todo_api.modules.tasks.search.TaskSort;

class TaskListParamsTest {

    @Test
    void defaults_hideCompletedAndSortByDueDateAscending() {
        TaskListParams params = new TaskListParams(null, null, null, null, null, null, null, null, null);

        assertThat(params.showCompleted()).isFalse();
        assertThat(params.sort()).isEqualTo(TaskSort.DUE_DATE);
        assertThat(params.direction()).isEqualTo(Direction.ASC);
        assertThat(params.tz()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(params.page()).isZero();
        assertThat(params.size()).isEqualTo(20);
    }

    /** FR-03.1 AC3: từ khóa rỗng thì không tìm. */
    @Test
    void blankKeyword_meansNoSearch() {
        assertThat(withKeyword("   ").q()).isNull();
        assertThat(withKeyword("  assign ").q()).isEqualTo("assign");
    }

    /** FR-03.3 AC2: chọn Priority mà không chọn chiều thì HIGH → LOW. */
    @Test
    void sortByPriority_defaultsToHighFirst() {
        TaskListParams params = new TaskListParams(null, null, null, null, TaskSort.PRIORITY, null, null, null, null);

        assertThat(params.direction()).isEqualTo(Direction.DESC);
    }

    private static TaskListParams withKeyword(String q) {
        return new TaskListParams(q, null, null, null, null, null, null, null, null);
    }
}
