import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';

/** Trang chính sau khi đăng nhập. */
export const HOME_PATH = '/inbox';

function Loading() {
  return <div className="page-center muted">Loading...</div>;
}

export function ProtectedRoute() {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}

/** Trang dành cho khách (login, register...): đã đăng nhập thì chuyển về trang chính. */
export function GuestRoute() {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  return user ? <Navigate to={HOME_PATH} replace /> : <Outlet />;
}
