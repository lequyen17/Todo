import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { Alert, AuthCard, Field } from '../components/Form';
import GoogleSignInButton from '../components/GoogleSignInButton';
import { confirmPasswordMatches, useForm } from '../hooks/useForm';
import { savePendingRegisterEmail } from './flowStorage';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { field, handleSubmit, error, submitting } = useForm(
    { displayName: '', email: '', password: '', confirmPassword: '' },
    { validate: confirmPasswordMatches('password') },
  );

  const register = async (values) => {
    await authApi.register(values);
    savePendingRegisterEmail(values.email.trim().toLowerCase());
    navigate('/register/verify');
  };

  return (
    <AuthCard
      title="Sign up"
      footer={
        <>
          Already have an account? <Link to="/login">Sign in</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(register)} noValidate>
        <Alert>{error}</Alert>
        <Field label="Display name (2-50 characters)" maxLength={50} {...field('displayName')} />
        <Field label="Email" type="email" autoComplete="email" {...field('email')} />
        <Field
          label="Password (8-72 characters, letters and numbers)"
          type="password"
          autoComplete="new-password"
          {...field('password')}
        />
        <Field label="Confirm password" type="password" autoComplete="new-password" {...field('confirmPassword')} />
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Sending code...' : 'Sign up'}
        </button>
      </form>
      <GoogleSignInButton text="signup_with" />
    </AuthCard>
  );
}
