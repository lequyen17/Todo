package com.example.todo_api.modules.tasks.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.tasks.TaskErrorCode;
import com.example.todo_api.modules.tasks.dto.CreateTaskRequest;
import com.example.todo_api.modules.tasks.dto.TaskListParams;
import com.example.todo_api.modules.tasks.dto.UpdateTaskRequest;
import com.example.todo_api.modules.tasks.entity.Task;
import com.example.todo_api.modules.tasks.repository.TaskRepository;
import com.example.todo_api.modules.tasks.repository.TaskSearchRepository;
import com.example.todo_api.modules.tasks.search.TaskSearch;

/** Quản lý task trong Inbox (FR-02). Mọi thao tác chỉ chạm task của chính user (BR-01, BR-02). */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskSearchRepository searchRepository;

    public TaskService(TaskRepository taskRepository, TaskSearchRepository searchRepository) {
        this.taskRepository = taskRepository;
        this.searchRepository = searchRepository;
    }

    /** Danh sách Inbox kèm tìm kiếm, lọc, sắp xếp (FR-02.2, FR-03). */
    @Transactional(readOnly = true)
    public Page<Task> list(Long userId, TaskListParams params) {
        TaskSearch search = new TaskSearch(userId, params.q(), params.showCompleted(), params.priority(), params.due(),
                params.sort(), params.direction() == TaskListParams.Direction.ASC, zone(params.tz()));
        return searchRepository.search(search, Instant.now(), PageRequest.of(params.page(), params.size()));
    }

    @Transactional(readOnly = true)
    public long countIncomplete(Long userId) {
        return taskRepository.countByUserIdAndCompletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public Task get(Long userId, Long taskId) {
        return find(userId, taskId);
    }

    @Transactional
    public Task create(Long userId, CreateTaskRequest req) {
        Task task = Task.create(userId, req.title(), req.description(), req.priority(), req.dueDate(),
                req.dueHasTime(), req.reminderAt(), Instant.now());
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long userId, Long taskId, UpdateTaskRequest req) {
        Task task = find(userId, taskId);
        task.update(req.title(), req.description(), req.priority(), req.dueDate(), req.dueHasTime(),
                req.reminderAt(), Instant.now());
        return task;
    }

    @Transactional
    public Task setCompleted(Long userId, Long taskId, boolean completed) {
        Task task = find(userId, taskId);
        task.setCompleted(completed, Instant.now());
        return task;
    }

    /** Xóa vĩnh viễn; reminder chưa gửi mất theo task, thông báo của task bị xóa theo (ON DELETE CASCADE). */
    @Transactional
    public void delete(Long userId, Long taskId) {
        taskRepository.delete(find(userId, taskId));
    }

    private static ZoneId zone(String tz) {
        try {
            return ZoneId.of(tz);
        } catch (DateTimeException e) {
            throw new ApiException(TaskErrorCode.INVALID_TIME_ZONE, tz);
        }
    }

    private Task find(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ApiException(TaskErrorCode.TASK_NOT_FOUND));
    }
}
