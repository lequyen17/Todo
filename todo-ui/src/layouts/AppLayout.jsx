import { Link, Outlet } from 'react-router-dom';
import { HOME_PATH } from '../auth/RouteGuards';
import { CheckSquareIcon } from '../components/Icons';
import { UserMenu } from '../components/UserMenu';

/** Khung chung cho các trang sau khi đăng nhập: header + nội dung. Logo về trang Inbox (FR-05.1). */
export default function AppLayout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header-inner">
          <Link to={HOME_PATH} className="brand">
            <CheckSquareIcon size={22} />
            Todo
          </Link>
          <UserMenu />
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
