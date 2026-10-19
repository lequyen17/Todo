import { api } from './client';

/** Các endpoint /settings (cài đặt nhắc nhở). */
export const settingsApi = {
  get: () => api('/settings'),

  /** { reminderEnabled, channelWeb, channelEmail, remindBeforeMinutes } */
  update: (body) => api('/settings', { method: 'PUT', body }),
};
