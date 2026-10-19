import { useNotifications } from '../realtime/NotificationProvider';
import { BellIcon } from './Icons';

/** Toast khi có thông báo mới (FR-06.2 AC1): bấm để mở task, tự ẩn sau vài giây. */
export function Toaster() {
  const { toasts, openNotification, dismissToast } = useNotifications();
  if (toasts.length === 0) return null;

  return (
    <div className="toaster" role="status" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className="toast">
          <button type="button" className="toast-body" onClick={() => openNotification(t)}>
            <BellIcon size={16} />
            <span>{t.message}</span>
          </button>
          <button type="button" className="toast-close" onClick={() => dismissToast(t.id)} aria-label="Dismiss">
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
