import { api } from './client';

/** Các endpoint /notifications (thông báo trong web, FR-04.3). */
export const notificationsApi = {
  /** Mới nhất trước: { items: [{ id, taskId, message, read, createdAt }], page, totalPages, ... } */
  list: ({ page = 0, size = 10 } = {}) => api(`/notifications?page=${page}&size=${size}`),

  /** { count } — số trên chuông. */
  unreadCount: () => api('/notifications/unread-count'),

  markRead: (id) => api(`/notifications/${id}/read`, { method: 'PATCH' }),

  markAllRead: () => api('/notifications/read-all', { method: 'POST' }),
};
