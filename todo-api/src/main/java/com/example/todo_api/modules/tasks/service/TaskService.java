package com.example.todo_api.modules.tasks.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.tasks.TaskErrorCode;
import com.example.todo_api.modules.tasks.dto.CreateTaskRequest;
import com.example.todo_api.modules.tasks.dto.UpdateTaskRequest;
import com.example.todo_api.modules.tasks.entity.Task;
import com.example.todo_api.modules.tasks.repository.TaskRepository;

/** Quản lý task trong Inbox (FR-02). Mọi thao tác chỉ chạm task của chính user (BR-01, BR-02). */
@Service
public class TaskService {

    /**
     * Mặc định của Inbox: due date tăng dần, task không có due date xếp cuối (FR-03.3).
     * id phá hòa để thứ tự cố định giữa các trang.
     */
    private static final Sort INBOX_ORDER = Sort.by(Sort.Order.asc("dueDate").nullsLast(), Sort.Order.asc("id"));

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /** Mặc định ẩn task đã hoàn thành (FR-02.2). */
    @Transactional(readOnly = true)
    public Page<Task> list(Long userId, boolean showCompleted, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, INBOX_ORDER);
        return showCompleted
                ? taskRepository.findByUserId(userId, pageable)
                : taskRepository.findByUserIdAndCompletedFalse(userId, pageable);
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

    private Task find(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ApiException(TaskErrorCode.TASK_NOT_FOUND));
    }
}
