// Tìm / lọc / sắp xếp của Inbox (FR-03) nằm trên URL (?q=&showCompleted=&priority=&due=&sort=&direction=&page=),
// để F5, back/forward và ô tìm kiếm trên header dùng chung một nguồn.

export const PRIORITY_OPTIONS = [
  { value: '', label: 'All priorities' },
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW', label: 'Low' },
];

export const DUE_OPTIONS = [
  { value: '', label: 'Any due date' },
  { value: 'TODAY', label: 'Today' },
  { value: 'OVERDUE', label: 'Overdue' },
  { value: 'UPCOMING', label: 'Upcoming (7 days)' },
  { value: 'NO_DUE_DATE', label: 'No due date' },
];

export const SORT_OPTIONS = [
  { value: 'DUE_DATE', label: 'Due date' },
  { value: 'PRIORITY', label: 'Priority' },
  { value: 'CREATED_AT', label: 'Created date' },
];

const DEFAULT_SORT = 'DUE_DATE';

/** Chiều mặc định của từng tiêu chí, khớp server: priority HIGH → LOW, created date mới nhất trước. */
const DEFAULT_DIRECTION = { DUE_DATE: 'ASC', PRIORITY: 'DESC', CREATED_AT: 'DESC' };

export function readTaskQuery(searchParams) {
  const sort = searchParams.get('sort') || DEFAULT_SORT;
  return {
    // Giữ nguyên khoảng trắng để ô tìm kiếm gõ được từ khóa nhiều chữ; server tự bỏ khoảng trắng đầu/cuối
    q: searchParams.get('q') || '',
    // Mặc định ẩn task đã hoàn thành (FR-02.2)
    showCompleted: searchParams.get('showCompleted') === 'true',
    priority: searchParams.get('priority') || '',
    due: searchParams.get('due') || '',
    sort,
    direction: searchParams.get('direction') || DEFAULT_DIRECTION[sort],
    page: Math.max(0, Number(searchParams.get('page')) || 0),
  };
}

/** Có đang tìm hoặc lọc khác mặc định không (để hiện "Clear filters"). */
export function hasActiveFilters(query) {
  return Boolean(query.q.trim() || query.priority || query.due);
}

/**
 * URLSearchParams mới sau khi đổi một số giá trị. Giá trị bằng mặc định thì bỏ khỏi URL cho gọn;
 * đổi bất kỳ điều kiện nào thì về trang đầu, trừ khi chính là đổi trang.
 */
export function withChanges(searchParams, changes) {
  const next = new URLSearchParams(searchParams);
  Object.entries(changes).forEach(([key, value]) => {
    const isDefault =
      value === '' || value === null || value === undefined || value === false ||
      (key === 'sort' && value === DEFAULT_SORT) ||
      (key === 'page' && value === 0);
    if (isDefault) next.delete(key);
    else next.set(key, value);
  });
  if (!('page' in changes)) next.delete('page');
  // Đổi tiêu chí sắp xếp thì dùng lại chiều mặc định của tiêu chí mới
  if ('sort' in changes && !('direction' in changes)) next.delete('direction');
  return next;
}
