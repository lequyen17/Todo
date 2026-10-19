import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { HOME_PATH } from '../auth/RouteGuards';
import { withChanges } from '../pages/taskQuery';
import { SearchIcon } from './Icons';

/**
 * Ô tìm kiếm trên header (FR-05.1, FR-03.1): tìm ngay khi gõ trong title / description của task.
 * Từ khóa nằm trên URL của Inbox; đang ở trang khác thì gõ sẽ mở Inbox với từ khóa đó.
 * Xóa hết chữ thì bỏ tìm kiếm (FR-03.1 AC3: từ khóa rỗng thì không tìm).
 */
export function HeaderSearch() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const onInbox = location.pathname === HOME_PATH;
  const value = onInbox ? searchParams.get('q') || '' : '';

  const change = (e) => {
    const next = withChanges(onInbox ? searchParams : new URLSearchParams(), { q: e.target.value });
    // Đang ở Inbox: thay URL hiện tại, để mỗi phím gõ không thành một mục trong lịch sử back
    navigate(`${HOME_PATH}?${next}`, { replace: onInbox });
  };

  return (
    <form className="header-search" role="search" onSubmit={(e) => e.preventDefault()}>
      <SearchIcon size={16} />
      <input
        type="search"
        value={value}
        onChange={change}
        placeholder="Search tasks"
        maxLength={200}
        aria-label="Search tasks"
      />
    </form>
  );
}
