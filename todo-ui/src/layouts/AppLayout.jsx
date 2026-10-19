import { Link, Outlet } from 'react-router-dom';
import { HOME_PATH } from '../auth/RouteGuards';
import { HeaderSearch } from '../components/HeaderSearch';
import { CheckSquareIcon } from '../components/Icons';
import { NotificationBell } from '../components/NotificationBell';
import { Toaster } from '../components/Toaster';
import { UserMenu } from '../components/UserMenu';
import { NotificationProvider } from '../realtime/NotificationProvider';

/**
 * Khung chung cho các trang sau khi đăng nhập. Header (FR-05.1): logo về Inbox, ô tìm kiếm,
 * chuông thông báo, menu user. Kết nối realtime (FR-06) sống cùng khung này: đăng xuất thì ngắt.
 */
export default function AppLayout() {
  return (
    <NotificationProvider>
      <div className="app-shell">
        <header className="app-header">
          <div className="app-header-inner">
            <div className="app-header-start">
              <Link to={HOME_PATH} className="brand">
                <CheckSquareIcon size={22} />
                Todo
              </Link>
              <HeaderSearch />
            </div>
            <div className="app-header-end">
              <NotificationBell />
              <UserMenu />
            </div>
          </div>
        </header>
        <main className="app-main">
          <Outlet />
        </main>
        <Toaster />
      </div>
    </NotificationProvider>
  );
}
