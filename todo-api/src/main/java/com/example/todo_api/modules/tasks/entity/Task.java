package com.example.todo_api.modules.tasks.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.tasks.TaskErrorCode;

/**
 * Task trong Inbox của người tạo (BR-01). Trạng thái chỉ đổi qua các hành vi có tên, để các trường đi cùng nhau
 * (isCompleted + completedAt, dueDate + reminder) luôn khớp với ràng buộc của bảng tasks.
 * Mọi thời điểm lưu ở UTC; due date chỉ có ngày thì client gửi 23:59 giờ local đã quy ra UTC (BR-10, BR-11).
 */
@Entity
@Table(name = "tasks")
public class Task {

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "due_has_time", nullable = false)
    private boolean dueHasTime;

    @Column(name = "reminder_at")
    private Instant reminderAt;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Cho JPA. */
    protected Task() {
    }

    // ---------------------------------------------------------------- Tạo

    /**
     * reminderAt là giá trị user chốt trên form: client điền sẵn dueDate − "Nhắc trước" (FR-04.1), user có thể
     * sửa hoặc xóa. Kiểm tra theo cùng quy tắc với update (VR-10).
     */
    public static Task create(Long userId, String title, String description, Priority priority, Instant dueDate,
            boolean dueHasTime, Instant reminderAt, Instant now) {
        Task task = new Task();
        task.userId = userId;
        task.update(title, description, priority == null ? Priority.MEDIUM : priority, dueDate, dueHasTime,
                reminderAt, now);
        return task;
    }

    // ---------------------------------------------------------------- Sửa

    /**
     * Sửa nội dung, không đổi isCompleted / completedAt (FR-02.3). Kiểm tra hết rồi mới gán, lỗi thì task giữ nguyên.
     * <ul>
     * <li>Xóa due date: reminder bị xóa theo; nhưng đặt reminder mới cho task không có due date là lỗi.</li>
     * <li>Reminder không được muộn hơn due date, kể cả khi chỉ sửa due date (FR-04.1 AC4).</li>
     * <li>Chỉ reminder mới đặt phải ở tương lai, để sửa title của task có reminder đã qua vẫn được.</li>
     * <li>Đặt lại reminder: quay về trạng thái chưa gửi.</li>
     * </ul>
     */
    public void update(String title, String description, Priority priority, Instant dueDate, boolean dueHasTime,
            Instant reminderAt, Instant now) {
        if (dueDate == null) {
            if (reminderAt != null && !reminderAt.equals(this.reminderAt)) {
                throw new ApiException(TaskErrorCode.REMINDER_REQUIRES_DUE_DATE);
            }
            reminderAt = null;
        }
        boolean reminderChanged = !Objects.equals(reminderAt, this.reminderAt);
        if (reminderAt != null) {
            if (reminderChanged && !reminderAt.isAfter(now)) {
                throw new ApiException(TaskErrorCode.REMINDER_IN_PAST);
            }
            if (reminderAt.isAfter(dueDate)) {
                throw new ApiException(TaskErrorCode.REMINDER_AFTER_DUE_DATE);
            }
        }

        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.dueHasTime = dueDate != null && dueHasTime;
        if (reminderChanged) {
            this.reminderAt = reminderAt;
            this.reminderSent = false;
        }
    }

    // ---------------------------------------------------------------- Hoàn thành (BR-12)

    /** Tick: completedAt = now; bỏ tick: xóa completedAt. Gửi lại đúng trạng thái hiện tại thì giữ nguyên completedAt. */
    public void setCompleted(boolean completed, Instant now) {
        if (this.completed == completed) {
            return;
        }
        this.completed = completed;
        this.completedAt = completed ? now : null;
    }

    /** BR-07: chưa hoàn thành và đã qua due date. */
    public boolean isOverdue(Instant now) {
        return !completed && dueDate != null && dueDate.isBefore(now);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public boolean isDueHasTime() {
        return dueHasTime;
    }

    public Instant getReminderAt() {
        return reminderAt;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
