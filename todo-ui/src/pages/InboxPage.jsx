import { useCallback, useEffect, useState } from 'react';
import { settingsApi } from '../api/settings';
import { tasksApi } from '../api/tasks';
import { Alert } from '../components/Form';
import { PlusIcon } from '../components/Icons';
import { ConfirmDialog, Modal } from '../components/Modal';
import { TaskForm } from '../components/TaskForm';
import { formatDue } from '../utils/format';

const PAGE_SIZE = 20;

function TaskRow({ task, onToggle, onOpen }) {
  const [busy, setBusy] = useState(false);

  const toggle = async () => {
    setBusy(true);
    try {
      await onToggle(task);
    } finally {
      setBusy(false);
    }
  };

  return (
    <li className={`task-row${task.completed ? ' is-completed' : ''}`}>
      <input
        type="checkbox"
        className="task-check"
        checked={task.completed}
        onChange={toggle}
        disabled={busy}
        aria-label={task.completed ? `Mark "${task.title}" as not completed` : `Complete "${task.title}"`}
      />
      <button type="button" className="task-main" onClick={() => onOpen(task)}>
        <span className="task-title">{task.title}</span>
        <span className="task-meta">
          <span className={`priority priority-${task.priority.toLowerCase()}`}>{task.priority}</span>
          {task.dueDate && (
            <span className={`task-due${task.overdue ? ' is-overdue' : ''}`}>{formatDue(task)}</span>
          )}
        </span>
      </button>
    </li>
  );
}

/** Inbox: trang chính sau khi đăng nhập (FR-02). */
export default function InboxPage() {
  const [incompleteCount, setIncompleteCount] = useState(null);
  const [showCompleted, setShowCompleted] = useState(false);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [settings, setSettings] = useState(null);
  // null: đóng; 'new': form tạo đầy đủ; object task: form sửa
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await tasksApi.list({ showCompleted, page, size: PAGE_SIZE });
      // Trang hiện tại vừa trống (hoàn thành / xóa task cuối trang) -> lùi về trang trước
      if (data.items.length === 0 && page > 0) {
        setPage(page - 1);
        return;
      }
      setResult(data);
      setError('');
    } catch (err) {
      setError(err.message);
    }
  }, [showCompleted, page]);

  useEffect(() => {
    load();
  }, [load]);

  // "Nhắc trước" để form tạo task điền sẵn reminder; lỗi thì form chỉ không điền sẵn
  useEffect(() => {
    settingsApi.get().then(setSettings).catch(() => setSettings(null));
  }, []);

  // Số task chưa hoàn thành cạnh tiêu đề Inbox (FR-02.0)
  const loadCount = useCallback(() => {
    tasksApi
      .count()
      .then((res) => setIncompleteCount(res.incomplete))
      .catch(() => setIncompleteCount(null));
  }, []);

  useEffect(() => {
    loadCount();
  }, [loadCount]);

  const afterChange = async () => {
    await load();
    loadCount();
  };

  const toggle = async (task) => {
    try {
      await tasksApi.setCompleted(task.id, !task.completed);
      await afterChange();
    } catch (err) {
      setError(err.message);
    }
  };

  const save = async (body) => {
    if (editing === 'new') {
      await tasksApi.create(body);
    } else {
      await tasksApi.update(editing.id, body);
    }
    setEditing(null);
    await afterChange();
  };

  const confirmDelete = async () => {
    setDeleteBusy(true);
    try {
      await tasksApi.remove(deleting.id);
      setDeleting(null);
      setEditing(null);
      await afterChange();
    } catch (err) {
      setDeleting(null);
      setError(err.message);
    } finally {
      setDeleteBusy(false);
    }
  };

  // Hộp xác nhận xóa đang mở phía trên thì Esc chỉ đóng hộp đó, giữ form sửa
  const closeEditor = useCallback(() => {
    if (!deleting) setEditing(null);
  }, [deleting]);
  const cancelDelete = useCallback(() => setDeleting(null), []);

  return (
    <div className="page">
      <div className="page-header inbox-header">
        <h1>
          Inbox
          {incompleteCount !== null && <span className="inbox-count">{incompleteCount}</span>}
        </h1>
        <button type="button" className="btn btn-primary" onClick={() => setEditing('new')}>
          <PlusIcon size={16} /> New task
        </button>
      </div>

      <section className="card task-list-card">
        <div className="task-list-toolbar">
          <label className="toggle">
            <input
              type="checkbox"
              checked={showCompleted}
              onChange={(e) => {
                setShowCompleted(e.target.checked);
                setPage(0);
              }}
            />
            Show completed
          </label>
        </div>

        <Alert>{error}</Alert>
        {!result && !error && <p className="muted task-empty">Loading...</p>}
        {result && result.items.length === 0 && (
          <p className="muted task-empty">
            {showCompleted ? 'No tasks yet.' : 'Nothing to do. Create a task with New task.'}
          </p>
        )}
        {result && result.items.length > 0 && (
          <ul className="task-list">
            {result.items.map((task) => (
              <TaskRow key={task.id} task={task} onToggle={toggle} onOpen={setEditing} />
            ))}
          </ul>
        )}

        {result && result.totalPages > 1 && (
          <div className="pagination">
            <button type="button" className="btn btn-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span className="muted">
              Page {page + 1} of {result.totalPages}
            </span>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={page + 1 >= result.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        )}
      </section>

      {editing && (
        <Modal title={editing === 'new' ? 'New task' : 'Edit task'} onClose={closeEditor}>
          <TaskForm
            task={editing === 'new' ? null : editing}
            settings={settings}
            submitLabel={editing === 'new' ? 'Create task' : 'Save changes'}
            onSubmit={save}
            onCancel={closeEditor}
            onDelete={editing === 'new' ? undefined : () => setDeleting(editing)}
          />
        </Modal>
      )}

      {deleting && (
        <ConfirmDialog
          title="Delete task"
          message="Are you sure you want to delete this task?"
          busy={deleteBusy}
          onConfirm={confirmDelete}
          onCancel={cancelDelete}
        />
      )}
    </div>
  );
}
