import { Link } from 'react-router-dom';
import { authApi } from '../api/auth';
import { Alert, AuthCard, Field } from '../components/Form';
import { useForm } from '../hooks/useForm';

export default function ForgotPasswordPage() {
  const { field, handleSubmit, error, success, submitting } = useForm({ email: '' });

  const sendLink = async ({ email }) => {
    const res = await authApi.forgotPassword(email);
    return res.message;
  };

  return (
    <AuthCard
      title="Forgot password"
      subtitle="Enter your registered email and we'll send you a password reset link."
      footer={<Link to="/login">Back to sign in</Link>}
    >
      {success ? (
        <Alert type="success">{success}</Alert>
      ) : (
        <form onSubmit={handleSubmit(sendLink)} noValidate>
          <Alert>{error}</Alert>
          <Field label="Email" type="email" autoComplete="email" {...field('email')} />
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Sending...' : 'Send reset link'}
          </button>
        </form>
      )}
    </AuthCard>
  );
}
