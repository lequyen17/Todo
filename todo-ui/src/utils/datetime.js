// Chuyển giữa thời điểm ISO (UTC, như API trả về) và giá trị của <input type="date|time|datetime-local"> (giờ local)

const pad = (n) => String(n).padStart(2, '0');

export function toDateInput(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function toTimeInput(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function toDateTimeInput(iso) {
  return iso ? `${toDateInput(iso)}T${toTimeInput(iso)}` : '';
}

/** Due date từ ô ngày + ô giờ; chỉ có ngày thì giờ là 23:59 local (BR-11). */
export function dueFromInputs(date, time) {
  return date ? new Date(`${date}T${time || '23:59'}`).toISOString() : null;
}

export function fromDateTimeInput(value) {
  return value ? new Date(value).toISOString() : null;
}
