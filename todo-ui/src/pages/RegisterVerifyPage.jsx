import { Link, Navigate, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import { HOME_PATH } from '../auth/RouteGuards';
import { AuthCard } from '../components/Form';
import { OtpForm } from '../components/OtpForm';
import { clearPendingRegisterEmail, loadPendingRegisterEmail } from './flowStorage';

export default function RegisterVerifyPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const email = loadPendingRegisterEmail();

  if (!email) return <Navigate to="/register" replace />;

  const verify = async (otp) => {
    const tokens = await authApi.verifyRegister(email, otp);
    clearPendingRegisterEmail();
    await signIn(tokens);
    navigate(HOME_PATH, { replace: true });
  };

  const resend = () => authApi.resendRegisterOtp(email);

  return (
    <AuthCard
      title="Verify your email"
      subtitle={`Enter the 6-digit code sent to ${email}`}
      footer={<Link to="/register">Use a different email</Link>}
    >
      <OtpForm onSubmit={verify} onResend={resend} />
    </AuthCard>
  );
}
