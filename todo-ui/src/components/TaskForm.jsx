import { useRef } from 'react';
import { useForm } from '../hooks/useForm';
import {
  dueFromInputs,
  fromDateTimeInput,
  toDateInput,
  toDateTimeInput,
  toTimeInput,
} from '../utils/datetime';
import { formatDate } from '../utils/format';
import { Alert } from './Form';
import { TimeSelect } from './TimeSelect';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

// Lỗi reminder (VR-10) hiện dưới ô Reminder thay vì thành thông báo chung
const REMINDER_ERRORS = ['REMINDER_REQUIRES_DUE_DATE', 'REMINDER_IN_PAST', 'REMINDER_AFTER_DUE_DATE'];

function toInputs(task) {
  return {
    title: task?.title ?? '',
    description: task?.description ?? '',
    priority: task?.priority ?? 'MEDIUM',
    dueDate: toDateInput(task?.dueDate),
    dueTime: task?.dueHasTime ? toTimeInput(task.dueDate) : '',
    reminder: toDateTimeInput(task?.reminderAt),
  };
}

/** reminder = due − "Nhắc trước"; đã ở quá khứ thì để trống (FR-04.1). */
function prefillReminder(dueDate, dueTime, remindBeforeMinutes) {
  const due = dueFromInputs(dueDate, dueTime);
  if (!due) return '';
  const reminder = new Date(new Date(due).getTime() - remindBeforeMinutes * 60_000);
  return reminder > new Date() ? toDateTimeInput(reminder.toISOString()) : '';
}

const validate = (values) => (values.title.trim() ? null : { title: 'Title is required' });

/**
 * Form tạo / sửa task. task = null là tạo mới.
 * Tạo mới: chọn due date thì reminder tự điền theo "Nhắc trước" trong Settings, cho tới khi user tự sửa ô reminder.
 * Sửa: không tự điền lại; xóa due date thì xóa luôn reminder.
 */
export function TaskForm({ task, settings, submitLabel, onSubmit, onCancel, onDelete }) {
  const initial = useRef(toInputs(task)).current;
  const reminderTouched = useRef(false);
  const { values, setValues, field, handleSubmit, error, fieldErrors, submitting } = useForm(initial, { validate });
  const isNew = !task;

  // field() kèm cả error; ô nhập chỉ nhận value + onChange
  const input = (name) => {
    const { value, onChange } = field(name);
    return { value, onChange };
  };

  const changeDue = (name, value) => {
    const next = { ...values, [name]: value };
    if (!next.dueDate) {
      next.dueTime = '';
      next.reminder = '';
      // Bỏ due date rồi chọn lại thì điền sẵn lại như lúc đầu
      reminderTouched.current = false;
    } else if (isNew && !reminderTouched.current && settings) {
      next.reminder = prefillReminder(next.dueDate, next.dueTime, settings.remindBeforeMinutes);
    }
    setValues(next);
  };

  // values.reminder giữ dạng "YYYY-MM-DDTHH:mm"; trên form tách thành ô ngày + ô giờ
  const reminderDate = values.reminder.slice(0, 10);
  const reminderTime = values.reminder.slice(11);

  const changeReminder = (date, time) => {
    reminderTouched.current = true;
    // Chọn ngày trước khi chọn giờ: tạm lấy giờ của due date (hoặc 09:00)
    setValues({ ...values, reminder: date ? `${date}T${time || values.dueTime || '09:00'}` : '' });
  };

  const submit = async (v) => {
    // Ô nào user không đụng tới thì gửi lại đúng giá trị server đã trả, tránh lệch giây khi đổi qua input
    const dueUnchanged = !isNew && v.dueDate === initial.dueDate && v.dueTime === initial.dueTime;
    const reminderUnchanged = !isNew && v.reminder === initial.reminder;
    const body = {
      title: v.title,
      description: v.description,
      priority: v.priority,
      dueDate: dueUnchanged ? task.dueDate : dueFromInputs(v.dueDate, v.dueTime),
      dueHasTime: Boolean(v.dueDate && v.dueTime),
      reminderAt: reminderUnchanged ? task.reminderAt : fromDateTimeInput(v.reminder),
    };
    try {
      await onSubmit(body);
    } catch (err) {
      if (REMINDER_ERRORS.includes(err.code)) {
        err.fields = { reminder: err.message };
        err.message = '';
      }
      throw err;
    }
  };

  return (
    <form onSubmit={handleSubmit(submit)} noValidate>
      <Alert>{error}</Alert>

      <label className="field">
        <span>Title</span>
        <input autoFocus maxLength={200} {...input('title')} aria-invalid={Boolean(fieldErrors.title)} />
        {fieldErrors.title && <small className="field-error">{fieldErrors.title}</small>}
      </label>

      <label className="field">
        <span>Description</span>
        <textarea rows={3} maxLength={2000} {...input('description')} />
        {fieldErrors.description && <small className="field-error">{fieldErrors.description}</small>}
      </label>

      <label className="field">
        <span>Priority</span>
        <select {...input('priority')}>
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p.charAt(0) + p.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
      </label>

      <div className="field-row">
        <label className="field">
          <span>Due date</span>
          <input type="date" value={values.dueDate} onChange={(e) => changeDue('dueDate', e.target.value)} />
        </label>
        <div className="field">
          <span>Time (optional)</span>
          <TimeSelect
            label="Due"
            optional
            value={values.dueTime}
            onChange={(time) => changeDue('dueTime', time)}
            disabled={!values.dueDate}
          />
        </div>
      </div>

      <div className="field">
        <span>Reminder</span>
        <div className="field-row">
          <input
            type="date"
            value={reminderDate}
            onChange={(e) => changeReminder(e.target.value, reminderTime)}
            disabled={!values.dueDate}
            aria-label="Reminder date"
            aria-invalid={Boolean(fieldErrors.reminder)}
          />
          <TimeSelect
            label="Reminder"
            value={reminderTime}
            onChange={(time) => changeReminder(reminderDate || values.dueDate, time)}
            disabled={!values.dueDate}
          />
        </div>
        {fieldErrors.reminder && <small className="field-error">{fieldErrors.reminder}</small>}
        {values.dueDate && settings && !settings.reminderEnabled && (
          <small className="field-hint">Reminders are turned off in Settings, so none will be sent.</small>
        )}
      </div>

      {task && (
        // Các trường hệ thống tự set, để phần xem chi tiết đủ mọi trường của task (FR-02.2)
        <dl className="task-details">
          <div>
            <dt>Status</dt>
            <dd>{task.completed ? `Completed ${formatDate(task.completedAt)}` : 'Not completed'}</dd>
          </div>
          {task.reminderAt && (
            <div>
              <dt>Reminder</dt>
              <dd>{task.reminderSent ? 'Sent' : 'Not sent yet'}</dd>
            </div>
          )}
          <div>
            <dt>Created</dt>
            <dd>{formatDate(task.createdAt)}</dd>
          </div>
          <div>
            <dt>Last updated</dt>
            <dd>{formatDate(task.updatedAt)}</dd>
          </div>
        </dl>
      )}

      <div className="modal-actions">
        {onDelete && (
          <button type="button" className="btn btn-danger-ghost modal-actions-start" onClick={onDelete}>
            Delete
          </button>
        )}
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : submitLabel}
        </button>
      </div>
    </form>
  );
}
