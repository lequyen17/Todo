package com.example.todo_api.modules.tasks.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.todo_api.modules.tasks.entity.Task;

/**
 * Mọi truy vấn đều lọc theo userId: task của người khác coi như không tồn tại (BR-01, BR-02).
 * Tìm / lọc / sắp xếp danh sách nằm ở TaskSearchRepository.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndCompletedFalse(Long userId);

    /** Task còn tồn tại và chưa hoàn thành: kiểm tra lại ngay trước khi gửi nhắc nhở (FR-04.2 bước 4). */
    boolean existsByIdAndCompletedFalse(Long id);
}
