package com.example.todo_api.modules.tasks.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import com.example.todo_api.modules.tasks.entity.Priority;
import com.example.todo_api.modules.tasks.entity.Task;
import com.example.todo_api.modules.tasks.search.DueFilter;
import com.example.todo_api.modules.tasks.search.TaskSearch;

/**
 * Tìm / lọc / sắp xếp task (FR-03) bằng Criteria API: sắp theo priority cần CASE (HIGH → MEDIUM → LOW),
 * Sort của Spring Data chỉ sắp được theo cột.
 */
@Repository
public class TaskSearchRepository {

    @PersistenceContext
    private EntityManager em;

    public Page<Task> search(TaskSearch search, Instant now, PageRequest pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<Task> root = query.from(Task.class);
        query.where(predicates(cb, root, search, now)).orderBy(orders(cb, root, search));
        List<Task> items = em.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Task> countRoot = countQuery.from(Task.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countRoot, search, now));
        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(items, pageable, total);
    }

    private static Predicate[] predicates(CriteriaBuilder cb, Root<Task> root, TaskSearch search, Instant now) {
        List<Predicate> where = new ArrayList<>();
        // BR-13: chỉ task trong Inbox của chính user
        where.add(cb.equal(root.get("userId"), search.userId()));

        Path<Boolean> completed = root.get("completed");
        // FR-02.2: mặc định ẩn task đã hoàn thành
        if (!search.showCompleted()) {
            where.add(cb.isFalse(completed));
        }

        if (search.priority() != null) {
            where.add(cb.equal(root.get("priority"), search.priority()));
        }

        if (search.due() != null) {
            Path<Instant> dueDate = root.get("dueDate");
            if (search.due() == DueFilter.NO_DUE_DATE) {
                where.add(cb.isNull(dueDate));
            } else {
                where.add(cb.isFalse(completed));
                DueFilter.Range range = search.due().range(now, search.zone());
                if (range.from() != null) {
                    where.add(cb.greaterThanOrEqualTo(dueDate, range.from()));
                }
                where.add(cb.lessThan(dueDate, range.to()));
            }
        }

        // FR-03.1: title hoặc description chứa từ khóa, không phân biệt hoa thường
        if (search.keyword() != null) {
            String pattern = "%" + escapeLike(search.keyword().toLowerCase(Locale.ROOT)) + "%";
            where.add(cb.or(
                    cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("description")), pattern, '\\')));
        }
        return where.toArray(Predicate[]::new);
    }

    private static List<Order> orders(CriteriaBuilder cb, Root<Task> root, TaskSearch search) {
        boolean asc = search.ascending();
        Path<Instant> dueDate = root.get("dueDate");
        // Task không có due date luôn xếp cuối, bất kể chiều sắp (FR-03.3)
        Order noDueDateLast = cb.asc(cb.<Integer>selectCase().when(cb.isNull(dueDate), 1).otherwise(0));
        Order byId = cb.asc(root.get("id"));

        return switch (search.sort()) {
            case DUE_DATE -> List.of(noDueDateLast, direction(cb, dueDate, asc), byId);
            case PRIORITY -> List.of(direction(cb, priorityRank(cb, root), asc), noDueDateLast, cb.asc(dueDate), byId);
            case CREATED_AT -> List.of(direction(cb, root.get("createdAt"), asc), direction(cb, root.get("id"), asc));
        };
    }

    /** LOW = 1 < MEDIUM = 2 < HIGH = 3; giảm dần cho thứ tự HIGH → MEDIUM → LOW. */
    private static Expression<Integer> priorityRank(CriteriaBuilder cb, Root<Task> root) {
        Path<Priority> priority = root.get("priority");
        return cb.<Integer>selectCase()
                .when(cb.equal(priority, Priority.HIGH), 3)
                .when(cb.equal(priority, Priority.MEDIUM), 2)
                .otherwise(1);
    }

    private static Order direction(CriteriaBuilder cb, Expression<?> expression, boolean asc) {
        return asc ? cb.asc(expression) : cb.desc(expression);
    }

    /** Từ khóa chứa % hoặc _ được tìm đúng ký tự đó, không thành ký tự đại diện của LIKE. */
    static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
