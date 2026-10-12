const ACCESS_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';

export const tokenStore = {
  get access() {
    return localStorage.getItem(ACCESS_KEY);
  },
  get refresh() {
    return localStorage.getItem(REFRESH_KEY);
  },
  set({ accessToken, refreshToken }) {
    localStorage.setItem(ACCESS_KEY, accessToken);
    localStorage.setItem(REFRESH_KEY, refreshToken);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
  isRefreshKey(key) {
    return key === REFRESH_KEY;
  },
};

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || 'Something went wrong, please try again');
    this.status = status;
    this.code = body?.code;
    this.fields = body?.fields || {};
  }
}

/** Phát ra khi phiên đăng nhập không thể khôi phục (refresh thất bại). */
export const SESSION_EXPIRED_EVENT = 'auth:session-expired';

function send(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth && tokenStore.access) headers.Authorization = `Bearer ${tokenStore.access}`;
  return fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

async function parse(res) {
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}

/**
 * Refresh token bị rotate sau mỗi lần dùng, nên chỉ được refresh một lần cho mọi tab.
 * navigator.locks khoá chung giữa các tab; tab vào sau thấy access token đã đổi thì dùng luôn.
 */
async function refreshTokens(failedAccessToken) {
  const run = async () => {
    if (tokenStore.access && tokenStore.access !== failedAccessToken) return true;
    const refreshToken = tokenStore.refresh;
    if (!refreshToken) return false;
    const res = await send('/auth/refresh', { method: 'POST', body: { refreshToken }, auth: false });
    if (!res.ok) {
      tokenStore.clear();
      return false;
    }
    tokenStore.set(await res.json());
    return true;
  };
  return navigator.locks ? navigator.locks.request('todo-auth-refresh', run) : run();
}

export async function api(path, options = {}) {
  const auth = options.auth !== false;
  const usedAccessToken = tokenStore.access;
  let res = await send(path, options);

  if (res.status === 401 && auth && usedAccessToken) {
    if (await refreshTokens(usedAccessToken)) {
      res = await send(path, options);
    } else {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
  }
  return parse(res);
}
