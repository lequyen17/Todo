import { api } from './client';

/**
 * Các endpoint /auth. Page chỉ gọi hàm ở đây, không tự viết URL / method.
 * Refresh token nằm trong client.js vì đó là cơ chế giữ phiên, không phải thao tác của người dùng.
 */
const post = (path, body, { auth = false } = {}) => api(`/auth${path}`, { method: 'POST', body, auth });

export const authApi = {
  register: ({ displayName, email, password }) => post('/register', { displayName, email, password }),
  resendRegisterOtp: (email) => post('/register/resend', { email }),
  verifyRegister: (email, otp) => post('/register/verify', { email, otp }),

  /** Trả {mfaRequired, challenge, tokens}. */
  login: ({ email, password }) => post('/login', { email, password }),
  resendLoginOtp: (challengeId) => post('/login/resend', { challengeId }),
  verifyLogin: (challengeId, otp) => post('/login/verify', { challengeId, otp }),

  googleConfig: () => api('/auth/google/config', { auth: false }),
  loginWithGoogle: (credential) => post('/google', { credential }),

  logout: () => post('/logout', undefined, { auth: true }),

  changePassword: ({ currentPassword, newPassword }) =>
    post('/change-password', { currentPassword, newPassword }, { auth: true }),
  forgotPassword: (email) => post('/forgot-password', { email }),
  resetPassword: (token, newPassword) => post('/reset-password', { token, newPassword }),
};
