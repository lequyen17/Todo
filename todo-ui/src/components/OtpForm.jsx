import { useEffect, useState } from 'react';
import { useForm } from '../hooks/useForm';
import { Alert, Field } from './Form';

const RESEND_SECONDS = 60;

/** Form nhập OTP 6 số kèm nút gửi lại mã có đếm ngược. */
export function OtpForm({ onSubmit, onResend }) {
  const { values, setValue, handleSubmit, error, setError, fieldErrors, success, setSuccess, submitting } =
    useForm({ otp: '' });
  const [cooldown, setCooldown] = useState(RESEND_SECONDS);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = setTimeout(() => setCooldown((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const handleResend = async () => {
    setError('');
    setSuccess('');
    try {
      await onResend();
      setSuccess('A new verification code has been sent');
      setValue('otp', '');
      setCooldown(RESEND_SECONDS);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <form onSubmit={handleSubmit(({ otp }) => onSubmit(otp))} noValidate>
      <Alert>{error}</Alert>
      <Alert type="success">{success}</Alert>
      <Field
        label="Verification code"
        className="otp-input"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        autoFocus
        value={values.otp}
        onChange={(e) => setValue('otp', e.target.value.replace(/\D/g, ''))}
        error={fieldErrors.otp}
      />
      <button type="submit" className="btn btn-primary" disabled={submitting || values.otp.length !== 6}>
        {submitting ? 'Verifying...' : 'Verify'}
      </button>
      <button type="button" className="btn btn-link" onClick={handleResend} disabled={cooldown > 0}>
        {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
      </button>
    </form>
  );
}
