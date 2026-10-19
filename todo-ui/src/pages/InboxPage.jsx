import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { settingsApi } from '../api/settings';
import { tasksApi } from '../api/tasks';
import { Alert } from '../components/Form';
import { PlusIcon } from '../components/Icons';
import { ConfirmDialog, Modal } from '../components/Modal';
import { TaskForm } from '../components/TaskForm';
import { formatDue } from '../utils/format';
import {
  DUE_OPTIONS,
  PRIORITY_OPTIONS,
  SORT_OPTIONS,
  hasActiveFilters,
  readTaskQuery,
  withChanges,
} from './taskQuery';

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

function FilterSelect({ label, value, options, onChange }) {
  return (
    <label className="filter">
      <span>{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)}>
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </label>
  );
}

/** Inbox: trang chính sau khi đăng nhập (FR-02), kèm tìm kiếm / lọc / sắp xếp (FR-03). */
export default function InboxPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const query = readTaskQuery(searchParams);
  const queryKey = searchParams.toString();
  const [incompleteCount, setIncompleteCount] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [settings, setSettings] = useState(null);
  // null: đóng; 'new': form tạo đầy đủ; object task: form sửa
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const change = (changes) => setSearchParams((prev) => withChanges(prev, changes));
  const setPage = (page) => change({ page });

  const load = useCallback(async () => {
    const { page, ...filters } = readTaskQuery(new URLSearchParams(queryKey));
    try {
      const data = await tasksApi.list({ ...filters, page, size: PAGE_SIZE });
      // Trang hiện tại vừa trống (hoàn thành / xóa task cuối trang) -> lùi về trang trước
      if (data.items.length === 0 && page > 0) {
        setSearchParams((prev) => withChanges(prev, { page: page - 1 }));
        return;
      }
      setResult(data);
      setError('');
    } catch (err) {
      setError(err.message);
    }
  }, [queryKey, setSearchParams]);

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

      {query.q && (
        <div className="search-banner">
          <span>
            Results for <strong>"{query.q}"</strong>
          </span>
          <button type="button" className="btn btn-secondary" onClick={() => change({ q: '' })}>
            Clear search
          </button>
        </div>
      )}

      <section className="card task-list-card">
        <div className="task-list-toolbar">
          <div className="task-filters">
            <FilterSelect
              label="Priority"
              value={query.priority}
              options={PRIORITY_OPTIONS}
              onChange={(priority) => change({ priority })}
            />
            <FilterSelect label="Due" value={query.due} options={DUE_OPTIONS} onChange={(due) => change({ due })} />
            <label className="toggle">
              <input
                type="checkbox"
                checked={query.showCompleted}
                onChange={(e) => change({ showCompleted: e.target.checked })}
              />
              Show completed
            </label>
          </div>
          <div className="task-sort">
            <FilterSelect label="Sort by" value={query.sort} options={SORT_OPTIONS} onChange={(sort) => change({ sort })} />
            <button
              type="button"
              className="btn btn-secondary sort-direction"
              onClick={() => change({ direction: query.direction === 'ASC' ? 'DESC' : 'ASC' })}
              aria-label={query.direction === 'ASC' ? 'Ascending, switch to descending' : 'Descending, switch to ascending'}
              title={query.direction === 'ASC' ? 'Ascending' : 'Descending'}
            >
              {query.direction === 'ASC' ? '↑' : '↓'}
            </button>
          </div>
        </div>

        <Alert>{error}</Alert>
        {!result && !error && <p className="muted task-empty">Loading...</p>}
        {result && result.items.length === 0 && (
          <div className="task-empty">
            {hasActiveFilters(query) ? (
              <>
                <p className="muted">No tasks match your search or filters.</p>
                <button type="button" className="btn btn-secondary" onClick={() => setSearchParams({})}>
                  Clear filters
                </button>
              </>
            ) : (
              <p className="muted">Nothing to do. Create a task with New task.</p>
            )}
          </div>
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
            <button
              type="button"
              className="btn btn-secondary"
              disabled={query.page === 0}
              onClick={() => setPage(query.page - 1)}
            >
              Previous
            </button>
            <span className="muted">
              Page {query.page + 1} of {result.totalPages}
            </span>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={query.page + 1 >= result.totalPages}
              onClick={() => setPage(query.page + 1)}
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
