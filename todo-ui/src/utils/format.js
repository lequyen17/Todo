// Giờ 24h, giống ô chọn giờ trên form (TimeSelect)
const dateFormat = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short', hourCycle: 'h23' });
const dayFormat = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' });

export function formatDate(value) {
  return value ? dateFormat.format(new Date(value)) : '—';
}

/** Due date chỉ có ngày (dueHasTime = false) thì không hiện giờ 23:59. */
export function formatDue(task) {
  if (!task.dueDate) return '';
  return (task.dueHasTime ? dateFormat : dayFormat).format(new Date(task.dueDate));
}
