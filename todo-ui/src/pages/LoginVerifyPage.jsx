import { Link, Navigate, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import { AuthCard } from '../components/Form';
import { OtpForm } from '../components/OtpForm';
import { clearLoginChallenge, loadLoginChallenge } from './flowStorage';

export default function LoginVerifyPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const challenge = loadLoginChallenge();

  if (!challenge) return <Navigate to="/login" replace />;

  const verify = async (otp) => {
    const tokens = await authApi.verifyLogin(challenge.challengeId, otp);
    clearLoginChallenge();
    await signIn(tokens);
    navigate('/profile', { replace: true });
  };

  const resend = () => authApi.resendLoginOtp(challenge.challengeId);

  return (
    <AuthCard
      title="Verify sign-in"
      subtitle={`Enter the 6-digit code sent to ${challenge.email}`}
      footer={<Link to="/login">Back to sign in</Link>}
    >
      <OtpForm onSubmit={verify} onResend={resend} />
    </AuthCard>
  );
}
