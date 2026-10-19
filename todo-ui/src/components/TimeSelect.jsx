// Chọn giờ dạng 24h (HH:mm). Không dùng <input type="time">: ô gốc của trình duyệt hiện AM/PM hay SA/CH
// tùy ngôn ngữ máy, lệch với cách app hiển thị giờ ở chỗ khác.

const pad = (n) => String(n).padStart(2, '0');
const HOURS = Array.from({ length: 24 }, (_, i) => pad(i));
const MINUTES = Array.from({ length: 60 }, (_, i) => pad(i));

/** value: "HH:mm" hoặc "" (chưa chọn). optional: cho chọn lại "--" để bỏ giờ. */
export function TimeSelect({ value, onChange, disabled, optional = false, label }) {
  const [hour, minute] = value ? value.split(':') : ['', ''];

  const changeHour = (h) => onChange(h ? `${h}:${minute || '00'}` : '');
  const changeMinute = (m) => onChange(`${hour}:${m}`);

  return (
    <div className="time-select">
      <select value={hour} onChange={(e) => changeHour(e.target.value)} disabled={disabled} aria-label={`${label} hour`}>
        {(optional || !hour) && <option value="">--</option>}
        {HOURS.map((h) => (
          <option key={h} value={h}>
            {h}
          </option>
        ))}
      </select>
      <span aria-hidden="true">:</span>
      <select
        value={minute}
        onChange={(e) => changeMinute(e.target.value)}
        disabled={disabled || !hour}
        aria-label={`${label} minute`}
      >
        {!hour && <option value="">--</option>}
        {MINUTES.map((m) => (
          <option key={m} value={m}>
            {m}
          </option>
        ))}
      </select>
    </div>
  );
}
