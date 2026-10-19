import { Client } from '@stomp/stompjs';
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tokenStore } from '../api/client';
import { notificationsApi } from '../api/notifications';

const NotificationContext = createContext(null);

const TOAST_MS = 5000;

function brokerUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}/api/ws`;
}

/**
 * Số trên chuông + kết nối realtime (FR-04.3, FR-06), dùng cho mọi trang sau khi đăng nhập.
 * - Mỗi lần kết nối / kết nối lại: lấy lại số chưa đọc trước, để không sót thông báo lúc mất kết nối (FR-06.1 AC2).
 * - Sự kiện chỉ có loại + id (FR-06.2): nhận xong thì gọi REST lấy dữ liệu, rồi tăng số và hiện toast.
 */
export function NotificationProvider({ children }) {
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);
  const [toasts, setToasts] = useState([]);
  // Tăng mỗi khi có thông báo mới, để danh sách trong chuông (nếu đang mở) tải lại
  const [version, setVersion] = useState(0);
  const toastTimers = useRef(new Map());

  const refreshUnread = useCallback(async () => {
    try {
      const res = await notificationsApi.unreadCount();
      setUnreadCount(res.count);
    } catch {
      // Lỗi mạng / phiên hết hạn: giữ số cũ, lần kết nối lại sau sẽ lấy lại
    }
  }, []);

  const dismissToast = useCallback((id) => {
    clearTimeout(toastTimers.current.get(id));
    toastTimers.current.delete(id);
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (notification) => {
      setToasts((list) => [...list.filter((t) => t.id !== notification.id), notification]);
      toastTimers.current.set(notification.id, setTimeout(() => dismissToast(notification.id), TOAST_MS));
    },
    [dismissToast],
  );

  const onNotificationCreated = useCallback(
    async (id) => {
      refreshUnread();
      setVersion((v) => v + 1);
      try {
        const page = await notificationsApi.list({ size: 5 });
        const created = page.items.find((n) => n.id === id);
        if (created) showToast(created);
      } catch {
        // Không lấy được nội dung thì chỉ tăng số trên chuông
      }
    },
    [refreshUnread, showToast],
  );

  /** Bấm một thông báo / toast: đánh dấu đã đọc và mở task liên quan (FR-04.3 AC3). */
  const openNotification = useCallback(
    async (notification) => {
      dismissToast(notification.id);
      navigate(`/inbox?task=${notification.taskId}`);
      if (!notification.read) {
        try {
          await notificationsApi.markRead(notification.id);
        } catch {
          // Task vẫn được mở; số trên chuông lấy lại bên dưới
        } finally {
          refreshUnread();
          setVersion((v) => v + 1);
        }
      }
    },
    [dismissToast, navigate, refreshUnread],
  );

  useEffect(() => {
    const client = new Client({
      brokerURL: brokerUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 20000,
      heartbeatOutgoing: 20000,
      // Gọi REST trước khi kết nối: vừa lấy lại số chưa đọc, vừa để client.js tự làm mới access token nếu đã hết hạn
      beforeConnect: async () => {
        await refreshUnread();
        client.connectHeaders = tokenStore.access ? { Authorization: `Bearer ${tokenStore.access}` } : {};
      },
      onConnect: () => {
        client.subscribe('/user/queue/events', (frame) => {
          const event = JSON.parse(frame.body);
          if (event.type === 'NOTIFICATION_CREATED') onNotificationCreated(event.id);
        });
      },
    });
    client.activate();
    const timers = toastTimers.current;
    return () => {
      client.deactivate();
      timers.forEach(clearTimeout);
    };
  }, [refreshUnread, onNotificationCreated]);

  const value = useMemo(
    () => ({ unreadCount, version, refreshUnread, openNotification, toasts, dismissToast }),
    [unreadCount, version, refreshUnread, openNotification, toasts, dismissToast],
  );
  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  return useContext(NotificationContext);
}
