import { Link, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/auth';
import { Alert, AuthCard, Field } from '../components/Form';
import { confirmPasswordMatches, useForm } from '../hooks/useForm';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const { field, handleSubmit, error, success, submitting } = useForm(
    { newPassword: '', confirmPassword: '' },
    { validate: confirmPasswordMatches('newPassword') },
  );

  const resetPassword = async ({ newPassword }) => {
    const res = await authApi.resetPassword(token, newPassword);
    return res.message;
  };

  return (
    <AuthCard title="Reset password" footer={<Link to="/login">Sign in</Link>}>
      {!token && <Alert>Invalid reset link.</Alert>}
      {success && <Alert type="success">{success}</Alert>}
      {token && !success && (
        <form onSubmit={handleSubmit(resetPassword)} noValidate>
          <Alert>{error}</Alert>
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
            {submitting ? 'Saving...' : 'Reset password'}
          </button>
        </form>
      )}
    </AuthCard>
  );
}
