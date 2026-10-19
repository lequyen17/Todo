package com.example.todo_api.modules.tasks.search;

/** Tiêu chí sắp xếp (FR-03.3). Task không có due date luôn xếp cuối khi sắp theo due date. */
public enum TaskSort {
    DUE_DATE, PRIORITY, CREATED_AT
}
