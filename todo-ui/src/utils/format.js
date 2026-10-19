// Giờ 24h, giống ô chọn giờ trên form (TimeSelect)
const dateFormat = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short', hourCycle: 'h23' });
const dayFormat = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' });

export function formatDate(value) {
  return value ? dateFormat.format(new Date(value)) : '—';
}

/** Thời gian tương đối cho thông báo: "Just now", "5 min ago", "3 h ago", "2 d ago"; quá 7 ngày thì hiện ngày giờ. */
export function formatRelative(value) {
  const seconds = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 1000));
  if (seconds < 60) return 'Just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)} min ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} h ago`;
  if (seconds < 7 * 86400) return `${Math.floor(seconds / 86400)} d ago`;
  return formatDate(value);
}

/** Due date chỉ có ngày (dueHasTime = false) thì không hiện giờ 23:59. */
export function formatDue(task) {
  if (!task.dueDate) return '';
  return (task.dueHasTime ? dateFormat : dayFormat).format(new Date(task.dueDate));
}
