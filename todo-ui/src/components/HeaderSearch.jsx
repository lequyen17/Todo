import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { HOME_PATH } from '../auth/RouteGuards';
import { withChanges } from '../pages/taskQuery';
import { SearchIcon } from './Icons';

/**
 * Ô tìm kiếm trên header (FR-05.1, FR-03.1): tìm trong title / description của task trong Inbox.
 * Đang ở Inbox thì giữ các bộ lọc hiện tại; ở trang khác thì mở Inbox với từ khóa.
 * Từ khóa rỗng thì không tìm (FR-03.1 AC3).
 */
export function HeaderSearch() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const onInbox = location.pathname === HOME_PATH;
  const current = onInbox ? searchParams.get('q') || '' : '';
  const [value, setValue] = useState(current);

  // Đồng bộ khi từ khóa đổi từ chỗ khác (nút Clear search, back/forward)
  useEffect(() => setValue(current), [current]);

  const submit = (e) => {
    e.preventDefault();
    const keyword = value.trim();
    if (!keyword) return;
    const next = withChanges(onInbox ? searchParams : new URLSearchParams(), { q: keyword });
    navigate(`${HOME_PATH}?${next}`);
  };

  return (
    <form className="header-search" role="search" onSubmit={submit}>
      <SearchIcon size={16} />
      <input
        type="search"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Search tasks"
        maxLength={200}
        aria-label="Search tasks"
      />
    </form>
  );
}
