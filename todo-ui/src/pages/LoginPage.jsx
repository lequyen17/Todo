import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import { Alert, AuthCard, Field } from '../components/Form';
import GoogleSignInButton from '../components/GoogleSignInButton';
import { useForm } from '../hooks/useForm';
import { saveLoginChallenge } from './flowStorage';

export default function LoginPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const { field, handleSubmit, error, submitting } = useForm({ email: '', password: '' });

  const login = async (values) => {
    const res = await authApi.login(values);
    if (res.mfaRequired) {
      saveLoginChallenge(res.challenge);
      navigate('/login/verify');
    } else {
      // Server tắt OTP đăng nhập (LOGIN_OTP_ENABLED=false) -> nhận token luôn
      await signIn(res.tokens);
      navigate('/profile', { replace: true });
    }
  };

  return (
    <AuthCard
      title="Sign in"
      footer={
        <>
          Don't have an account? <Link to="/register">Sign up</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(login)} noValidate>
        <Alert>{error}</Alert>
        <Field label="Email" type="email" autoComplete="email" {...field('email')} />
        <Field label="Password" type="password" autoComplete="current-password" {...field('password')} />
        <div className="row-end">
          <Link to="/forgot-password">Forgot password?</Link>
        </div>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Sending code...' : 'Continue'}
        </button>
      </form>
      <GoogleSignInButton text="signin_with" />
    </AuthCard>
  );
}
