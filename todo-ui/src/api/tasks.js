import { api } from './client';

/** Các endpoint /tasks (Inbox). */
export const tasksApi = {
  list: ({ showCompleted = false, page = 0, size = 20 } = {}) =>
    api(`/tasks?showCompleted=${showCompleted}&page=${page}&size=${size}`),

  /** { incomplete } — số hiện cạnh Inbox trên thanh điều hướng. */
  count: () => api('/tasks/count'),

  create: (body) => api('/tasks', { method: 'POST', body }),

  update: (id, body) => api(`/tasks/${id}`, { method: 'PUT', body }),

  setCompleted: (id, completed) => api(`/tasks/${id}/completion`, { method: 'PATCH', body: { completed } }),

  remove: (id) => api(`/tasks/${id}`, { method: 'DELETE' }),
};
