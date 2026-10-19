import { Link, Outlet } from 'react-router-dom';
import { HOME_PATH } from '../auth/RouteGuards';
import { HeaderSearch } from '../components/HeaderSearch';
import { CheckSquareIcon } from '../components/Icons';
import { UserMenu } from '../components/UserMenu';

/** Khung chung cho các trang sau khi đăng nhập: header (FR-05.1: logo về Inbox, ô tìm kiếm, menu user) + nội dung. */
export default function AppLayout() {
  return (
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
          <UserMenu />
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
