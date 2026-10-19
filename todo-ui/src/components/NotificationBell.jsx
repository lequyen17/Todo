import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { notificationsApi } from '../api/notifications';
import { useNotifications } from '../realtime/NotificationProvider';
import { formatRelative } from '../utils/format';
import { BellIcon } from './Icons';

const PAGE_SIZE = 10;

/** Chuông thông báo trên header (FR-05.1, FR-04.3): số chưa đọc, danh sách mới nhất trước, "Mark all as read". */
export function NotificationBell() {
  const { unreadCount, version, refreshUnread, openNotification } = useNotifications();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const containerRef = useRef(null);
  const location = useLocation();

  const loadPage = useCallback(async (nextPage) => {
    setLoading(true);
    setError('');
    try {
      const res = await notificationsApi.list({ page: nextPage, size: PAGE_SIZE });
      setItems((prev) => (nextPage === 0 ? res.items : [...prev, ...res.items]));
      setPage(nextPage);
      setHasMore(nextPage + 1 < res.totalPages);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // Mở chuông, hoặc có thông báo mới / vừa đọc trong lúc đang mở: tải lại trang đầu
  useEffect(() => {
    if (open) loadPage(0);
  }, [open, version, loadPage]);

  // Đóng khi chuyển trang, bấm ra ngoài hoặc nhấn Esc
  useEffect(() => setOpen(false), [location.pathname, location.search]);

  useEffect(() => {
    if (!open) return undefined;
    const onClick = (e) => {
      if (!containerRef.current?.contains(e.target)) setOpen(false);
    };
    const onKey = (e) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const markAllRead = async () => {
    try {
      await notificationsApi.markAllRead();
      setItems((list) => list.map((n) => ({ ...n, read: true })));
    } catch (err) {
      setError(err.message);
    } finally {
      refreshUnread();
    }
  };

  const badge = unreadCount > 99 ? '99+' : unreadCount;

  return (
    <div className="bell" ref={containerRef}>
      <button
        type="button"
        className="bell-trigger"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-label={unreadCount ? `Notifications, ${unreadCount} unread` : 'Notifications'}
        onClick={() => setOpen((v) => !v)}
      >
        <BellIcon size={20} />
        {unreadCount > 0 && <span className="bell-badge">{badge}</span>}
      </button>

      {open && (
        <div className="bell-panel" role="dialog" aria-label="Notifications">
          <div className="bell-panel-header">
            <strong>Notifications</strong>
            <button type="button" className="bell-mark-all" onClick={markAllRead} disabled={unreadCount === 0}>
              Mark all as read
            </button>
          </div>

          {error && <p className="bell-message field-error">{error}</p>}
          {!error && items.length === 0 && !loading && <p className="bell-message muted">No notifications yet.</p>}

          <ul className="bell-list">
            {items.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  className={`bell-item${n.read ? '' : ' is-unread'}`}
                  onClick={() => openNotification(n)}
                >
                  <span className="bell-item-message">{n.message}</span>
                  <span className="bell-item-time">{formatRelative(n.createdAt)}</span>
                </button>
              </li>
            ))}
          </ul>

          {loading && <p className="bell-message muted">Loading...</p>}
          {hasMore && !loading && (
            <button type="button" className="bell-more" onClick={() => loadPage(page + 1)}>
              Load more
            </button>
          )}
        </div>
      )}
    </div>
  );
}
