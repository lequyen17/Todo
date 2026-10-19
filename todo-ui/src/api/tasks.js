import { api } from './client';

/** Múi giờ trình duyệt: server dùng để tính "hôm nay" cho bộ lọc Due (BR-10). */
const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;

/** Các endpoint /tasks (Inbox). */
export const tasksApi = {
  /** params: { q, status, priority, due, sort, direction, page, size }; giá trị rỗng = mặc định của server. */
  list: (params = {}) => {
    const query = new URLSearchParams({ tz: timeZone });
    Object.entries(params).forEach(([key, value]) => {
      if (value !== '' && value !== null && value !== undefined) query.set(key, value);
    });
    return api(`/tasks?${query}`);
  },

  /** { incomplete } — số hiện cạnh tiêu đề Inbox. */
  count: () => api('/tasks/count'),

  get: (id) => api(`/tasks/${id}`),

  create: (body) => api('/tasks', { method: 'POST', body }),

  update: (id, body) => api(`/tasks/${id}`, { method: 'PUT', body }),

  setCompleted: (id, completed) => api(`/tasks/${id}/completion`, { method: 'PATCH', body: { completed } }),

  remove: (id) => api(`/tasks/${id}`, { method: 'DELETE' }),
};
