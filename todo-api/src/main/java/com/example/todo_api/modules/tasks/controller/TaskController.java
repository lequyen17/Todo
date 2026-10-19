package com.example.todo_api.modules.tasks.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.core.web.PageResponse;
import com.example.todo_api.modules.tasks.dto.CompletionRequest;
import com.example.todo_api.modules.tasks.dto.CreateTaskRequest;
import com.example.todo_api.modules.tasks.dto.TaskCountResponse;
import com.example.todo_api.modules.tasks.dto.TaskListParams;
import com.example.todo_api.modules.tasks.dto.TaskResponse;
import com.example.todo_api.modules.tasks.dto.UpdateTaskRequest;
import com.example.todo_api.modules.tasks.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** Inbox, 20 task mỗi trang (FR-02.2), kèm tìm kiếm / lọc / sắp xếp (FR-03); tham số xem TaskListParams. */
    @GetMapping
    PageResponse<TaskResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
            @Valid @ModelAttribute TaskListParams params) {
        Instant now = Instant.now();
        return PageResponse.from(taskService.list(principal.userId(), params), task -> TaskResponse.from(task, now));
    }

    @GetMapping("/count")
    TaskCountResponse count(@AuthenticationPrincipal AuthPrincipal principal) {
        return new TaskCountResponse(taskService.countIncomplete(principal.userId()));
    }

    @GetMapping("/{id}")
    TaskResponse get(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        return TaskResponse.from(taskService.get(principal.userId(), id), Instant.now());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(@AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateTaskRequest req) {
        return TaskResponse.from(taskService.create(principal.userId(), req), Instant.now());
    }

    @PutMapping("/{id}")
    TaskResponse update(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest req) {
        return TaskResponse.from(taskService.update(principal.userId(), id, req), Instant.now());
    }

    @PatchMapping("/{id}/completion")
    TaskResponse setCompletion(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody CompletionRequest req) {
        return TaskResponse.from(taskService.setCompleted(principal.userId(), id, req.completed()), Instant.now());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        taskService.delete(principal.userId(), id);
    }
}
