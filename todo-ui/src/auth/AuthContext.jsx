import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, SESSION_EXPIRED_EVENT, tokenStore } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadProfile = useCallback(async () => {
    const me = await api('/users/me');
    setUser(me);
    return me;
  }, []);

  useEffect(() => {
    if (!tokenStore.refresh) {
      setLoading(false);
      return;
    }
    loadProfile()
      .catch(() => {
        tokenStore.clear();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, [loadProfile]);

  useEffect(() => {
    const onExpired = () => setUser(null);
    // Tab khác đăng xuất -> refresh token bị xoá khỏi localStorage
    const onStorage = (e) => {
      if (tokenStore.isRefreshKey(e.key) && !e.newValue) setUser(null);
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
    window.addEventListener('storage', onStorage);
    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
      window.removeEventListener('storage', onStorage);
    };
  }, []);

  /** Gọi sau khi nhận cặp token (verify OTP, đổi mật khẩu). */
  const signIn = useCallback(
    async (tokens) => {
      tokenStore.set(tokens);
      return loadProfile();
    },
    [loadProfile],
  );

  const signOut = useCallback(async () => {
    try {
      await api('/auth/logout', { method: 'POST', body: { refreshToken: tokenStore.refresh } });
    } catch {
      // Dù server lỗi vẫn xoá phiên phía client
    }
    tokenStore.clear();
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, loading, signIn, signOut }), [user, loading, signIn, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
