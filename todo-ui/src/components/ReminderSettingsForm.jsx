import { useEffect, useState } from 'react';
import { settingsApi } from '../api/settings';
import { useForm } from '../hooks/useForm';
import { Alert } from './Form';

const UNITS = [
  { value: 'minutes', label: 'minutes', minutes: 1 },
  { value: 'hours', label: 'hours', minutes: 60 },
  { value: 'days', label: 'days', minutes: 1440 },
];
const MAX_MINUTES = 7 * 1440;

/** 120 phút -> 2 giờ; chọn đơn vị lớn nhất chia hết để hiển thị lại đúng cách user đã nhập. */
function splitMinutes(minutes) {
  const unit = [...UNITS].reverse().find((u) => minutes % u.minutes === 0);
  return { amount: String(minutes / unit.minutes), unit: unit.value };
}

function toMinutes(amount, unit) {
  return Number(amount) * UNITS.find((u) => u.value === unit).minutes;
}

function toValues(settings) {
  return {
    reminderEnabled: settings.reminderEnabled,
    channelWeb: settings.channelWeb,
    channelEmail: settings.channelEmail,
    ...splitMinutes(settings.remindBeforeMinutes),
  };
}

/** VR-12 và "bật thì phải chọn ít nhất một kênh" (FR-05.2), kiểm tra trước khi gọi API. */
function validate(values) {
  const errors = {};
  if (!/^\d+$/.test(values.amount) || Number(values.amount) < 1) {
    errors.remindBefore = 'Enter a whole number greater than 0';
  } else if (toMinutes(values.amount, values.unit) > MAX_MINUTES) {
    errors.remindBefore = 'Remind before must be at most 7 days';
  }
  if (values.reminderEnabled && !values.channelWeb && !values.channelEmail) {
    errors.channels = 'Choose at least one channel to receive reminders';
  }
  return Object.keys(errors).length ? errors : null;
}

function Checkbox({ label, checked, onChange, disabled }) {
  return (
    <label className="check-field">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} disabled={disabled} />
      <span>{label}</span>
    </label>
  );
}

function ReminderForm({ settings, onSaved }) {
  const { values, setValue, setValues, handleSubmit, error, fieldErrors, success, submitting } = useForm(
    toValues(settings),
    { validate },
  );

  const save = async (v) => {
    try {
      const saved = await settingsApi.update({
        reminderEnabled: v.reminderEnabled,
        channelWeb: v.channelWeb,
        channelEmail: v.channelEmail,
        remindBeforeMinutes: toMinutes(v.amount, v.unit),
      });
      setValues(toValues(saved));
      onSaved(saved);
      return 'Reminder settings saved.';
    } catch (err) {
      // Đưa lỗi của server về đúng ô trên form
      if (err.code === 'REMINDER_CHANNEL_REQUIRED') {
        err.fields = { channels: err.message };
        err.message = '';
      } else if (err.fields.remindBeforeMinutes) {
        err.fields = { remindBefore: err.fields.remindBeforeMinutes };
        err.message = '';
      }
      throw err;
    }
  };

  return (
    <form className="settings-form" onSubmit={handleSubmit(save)} noValidate>
      <Alert>{error}</Alert>
      <Alert type="success">{success}</Alert>

      <div className="settings-group">
        <Checkbox
          label="Receive reminders"
          checked={values.reminderEnabled}
          onChange={(checked) => setValue('reminderEnabled', checked)}
        />
        <p className="muted settings-group-hint">
          When off, reminders that come due are skipped and not sent later when you turn this back on.
        </p>
      </div>

      <fieldset className="settings-group" disabled={!values.reminderEnabled}>
        <legend>Channels</legend>
        <Checkbox label="In-app notification" checked={values.channelWeb} onChange={(c) => setValue('channelWeb', c)} />
        <Checkbox label="Email" checked={values.channelEmail} onChange={(c) => setValue('channelEmail', c)} />
        {fieldErrors.channels && <small className="field-error">{fieldErrors.channels}</small>}
      </fieldset>

      <div className="settings-group">
        <span className="settings-label">Remind before</span>
        <div className="remind-before">
          <input
            type="number"
            min={1}
            step={1}
            inputMode="numeric"
            value={values.amount}
            onChange={(e) => setValue('amount', e.target.value)}
            aria-label="Remind before amount"
            aria-invalid={Boolean(fieldErrors.remindBefore)}
          />
          <select value={values.unit} onChange={(e) => setValue('unit', e.target.value)} aria-label="Unit">
            {UNITS.map((u) => (
              <option key={u.value} value={u.value}>
                {u.label}
              </option>
            ))}
          </select>
        </div>
        {fieldErrors.remindBefore && <small className="field-error">{fieldErrors.remindBefore}</small>}
        <p className="muted settings-group-hint">
          Pre-fills the reminder when you create a task with a due date (up to 7 days). Existing reminders are not
          changed.
        </p>
      </div>

      <button type="submit" className="btn btn-primary" disabled={submitting}>
        {submitting ? 'Saving...' : 'Save settings'}
      </button>
    </form>
  );
}

/** Cài đặt nhắc nhở (FR-05.2). */
export function ReminderSettingsForm() {
  const [settings, setSettings] = useState(null);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    settingsApi
      .get()
      .then(setSettings)
      .catch((err) => setLoadError(err.message));
  }, []);

  if (loadError) return <Alert>{loadError}</Alert>;
  if (!settings) return <p className="muted">Loading...</p>;
  return <ReminderForm settings={settings} onSaved={setSettings} />;
}
