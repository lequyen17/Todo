import { useState } from 'react';
import { authApi } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import { Alert, Field } from '../components/Form';
import { BellIcon, LockIcon } from '../components/Icons';
import { ReminderSettingsForm } from '../components/ReminderSettingsForm';
import { confirmPasswordMatches, useForm } from '../hooks/useForm';

const EMPTY_FORM = { currentPassword: '', newPassword: '', confirmPassword: '' };

const SECTIONS = [
  { id: 'reminders', label: 'Reminders', icon: <BellIcon /> },
  { id: 'password', label: 'Password', icon: <LockIcon /> },
];

function ChangePasswordForm() {
  const { signIn } = useAuth();
  const { field, handleSubmit, setValues, error, success, submitting } = useForm(EMPTY_FORM, {
    validate: confirmPasswordMatches('newPassword'),
  });

  const changePassword = async (values) => {
    const tokens = await authApi.changePassword(values);
    // Server đã thu hồi mọi phiên khác, cấp phiên mới cho thiết bị này
    await signIn(tokens);
    setValues(EMPTY_FORM);
    return 'Password changed. You have been signed out on all other devices.';
  };

  return (
    <form className="settings-form" onSubmit={handleSubmit(changePassword)} noValidate>
      <Alert>{error}</Alert>
      <Alert type="success">{success}</Alert>
      <Field
        label="Current password"
        type="password"
        autoComplete="current-password"
        {...field('currentPassword')}
      />
      <Field
        label="New password (8-72 characters, letters and numbers)"
        type="password"
        autoComplete="new-password"
        {...field('newPassword')}
      />
      <Field
        label="Confirm new password"
        type="password"
        autoComplete="new-password"
        {...field('confirmPassword')}
      />
      <button type="submit" className="btn btn-primary" disabled={submitting}>
        {submitting ? 'Saving...' : 'Update password'}
      </button>
    </form>
  );
}

/**
 * Tài khoản tạo bằng Google chưa có mật khẩu: gửi link đặt mật khẩu về email của chính user,
 * dùng lại API quên mật khẩu (FR-01.4). Link mở trang /reset-password, vào được cả khi đang đăng nhập.
 */
function SetPasswordByEmail() {
  const { user } = useAuth();
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  const send = async () => {
    setSending(true);
    setError('');
    try {
      await authApi.forgotPassword(user.email);
      setSent(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="settings-form">
      <p className="muted card-description">
        Your account signs in with Google and has no password yet. To also sign in with email and password,
        we can send a link to <strong>{user.email}</strong> to set one.
      </p>
      <Alert>{error}</Alert>
      {sent && (
        <Alert type="success">
          We sent a link to {user.email}. Open it within 30 minutes to set your password.
        </Alert>
      )}
      <button type="button" className="btn btn-primary" onClick={send} disabled={sending}>
        {sending ? 'Sending...' : sent ? 'Send again' : 'Send me a link'}
      </button>
    </div>
  );
}

function PasswordSection() {
  const { user } = useAuth();

  return (
    <section className="card">
      <h2 className="card-title">{user.hasPassword ? 'Change password' : 'Set a password'}</h2>
      {user.hasPassword ? (
        <>
          <p className="muted card-description">
            After changing your password, you will be signed out on all other devices.
          </p>
          <ChangePasswordForm />
        </>
      ) : (
        <SetPasswordByEmail />
      )}
    </section>
  );
}

function RemindersSection() {
  return (
    <section className="card">
      <h2 className="card-title">Reminders</h2>
      <p className="muted card-description">Choose how you get reminded before a task is due.</p>
      <ReminderSettingsForm />
    </section>
  );
}

export default function SettingsPage() {
  const [active, setActive] = useState('reminders');

  return (
    <div className="page">
      <div className="page-header">
        <h1>Settings</h1>
        <p className="muted">Manage your reminders and account security.</p>
      </div>

      <div className="settings-layout">
        <nav className="settings-nav">
          {SECTIONS.map((s) => (
            <button
              key={s.id}
              type="button"
              className={`settings-nav-item${active === s.id ? ' active' : ''}`}
              aria-current={active === s.id ? 'page' : undefined}
              onClick={() => setActive(s.id)}
            >
              {s.icon} {s.label}
            </button>
          ))}
        </nav>

        {active === 'reminders' ? <RemindersSection /> : <PasswordSection />}
      </div>
    </div>
  );
}
