import { api, apiUpload } from './client';

/** Các endpoint /users. */
export const usersApi = {
  me: () => api('/users/me'),

  uploadAvatar: (file) => {
    const form = new FormData();
    form.append('file', file);
    return apiUpload('/users/me/avatar', form);
  },
};
