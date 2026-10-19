-- Phần quản lý công việc: cài đặt nhắc nhở, task, thông báo.

-- 1 user = 1 bộ cài đặt (PK cũng là FK tới users).
CREATE TABLE user_settings (
    user_id               BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    reminder_enabled      BOOLEAN     NOT NULL DEFAULT TRUE,
    channel_web           BOOLEAN     NOT NULL DEFAULT TRUE,
    channel_email         BOOLEAN     NOT NULL DEFAULT TRUE,
    remind_before_minutes INT         NOT NULL DEFAULT 60,      -- "Nhắc trước", quy đổi ra phút (VR-12)
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_settings_remind_before CHECK (remind_before_minutes BETWEEN 1 AND 10080),
    -- Bật nhắc nhở thì phải chọn ít nhất một kênh (FR-05.2).
    CONSTRAINT ck_user_settings_channel CHECK (NOT reminder_enabled OR channel_web OR channel_email)
);

CREATE TABLE tasks (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,   -- người tạo
    title          VARCHAR(200)  NOT NULL,
    description    VARCHAR(2000) NULL,
    priority       VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    is_completed   BOOLEAN       NOT NULL DEFAULT FALSE,
    due_date       TIMESTAMPTZ   NULL,                     -- lưu UTC
    due_has_time   BOOLEAN       NOT NULL DEFAULT FALSE,   -- FALSE -> hạn là 23:59 của ngày due_date
    reminder_at    TIMESTAMPTZ   NULL,
    reminder_sent  BOOLEAN       NOT NULL DEFAULT FALSE,
    completed_at   TIMESTAMPTZ   NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_tasks_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT ck_tasks_completed_at CHECK (is_completed = (completed_at IS NOT NULL)),
    -- Reminder cần due date và không muộn hơn due date (VR-10). "reminder_at > now" kiểm tra ở service.
    CONSTRAINT ck_tasks_reminder CHECK (reminder_at IS NULL OR (due_date IS NOT NULL AND reminder_at <= due_date))
);
-- Inbox (ẩn task đã xong, sắp theo due date, NULL xếp cuối), số task chưa xong, Dashboard Overdue/Today/Upcoming.
CREATE INDEX idx_tasks_user_completed_due ON tasks (user_id, is_completed, due_date);
-- Job nhắc nhở: chỉ chứa reminder còn chờ gửi, nên index luôn nhỏ.
CREATE INDEX idx_tasks_pending_reminder ON tasks (reminder_at)
    WHERE reminder_sent = FALSE AND is_completed = FALSE AND reminder_at IS NOT NULL;

CREATE TABLE notifications (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id    BIGINT       NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    message    VARCHAR(500) NOT NULL,   -- MSG-04
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_task_id ON notifications (task_id);
