import { useState } from 'react';

/**
 * State + submit chung cho form gọi API.
 * - field(name): props {value, onChange, error} để spread vào <Field>.
 * - handleSubmit(onSubmit): chặn submit mặc định, chạy validate phía client, gọi onSubmit(values);
 *   lỗi API (ApiError) -> error + fieldErrors; onSubmit trả về chuỗi -> hiện làm thông báo thành công.
 */
export function useForm(initialValues, { validate } = {}) {
  const [values, setValues] = useState(initialValues);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const setValue = (name, value) => setValues((prev) => ({ ...prev, [name]: value }));

  const field = (name) => ({
    value: values[name],
    onChange: (e) => setValue(name, e.target.value),
    error: fieldErrors[name],
  });

  const handleSubmit = (onSubmit) => async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    setSuccess('');
    const clientErrors = validate?.(values);
    if (clientErrors) {
      setFieldErrors(clientErrors);
      return;
    }
    setSubmitting(true);
    try {
      const message = await onSubmit(values);
      if (typeof message === 'string') setSuccess(message);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fields || {});
    } finally {
      setSubmitting(false);
    }
  };

  return {
    values,
    setValues,
    setValue,
    field,
    handleSubmit,
    error,
    setError,
    fieldErrors,
    success,
    setSuccess,
    submitting,
  };
}

/** validate cho form có ô nhập lại mật khẩu (confirmPassword). */
export const confirmPasswordMatches = (passwordField) => (values) =>
  values[passwordField] === values.confirmPassword ? null : { confirmPassword: 'Passwords do not match' };
