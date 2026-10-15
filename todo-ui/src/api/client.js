/** Access token chỉ trong RAM tab; refresh nằm HttpOnly cookie (path /api/auth). */
let accessToken = null;

const authChannel = typeof BroadcastChannel !== 'undefined' ? new BroadcastChannel('todo-auth') : null;

export const tokenStore = {
  get access() {
    return accessToken;
  },
  set(tokens) {
    accessToken = tokens?.accessToken ?? null;
    if (accessToken) broadcastSessionUpdated(accessToken);
  },
  clear() {
    accessToken = null;
    broadcastSignedOut();
  },
};

function broadcastSessionUpdated(token) {
  authChannel?.postMessage({ type: 'session-updated', accessToken: token });
}

function broadcastSignedOut() {
  authChannel?.postMessage({ type: 'signed-out' });
}

authChannel?.addEventListener('message', (event) => {
  const msg = event.data;
  if (msg?.type === 'session-updated') {
    accessToken = msg.accessToken;
  } else if (msg?.type === 'signed-out') {
    accessToken = null;
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
  }
});

/** Body lỗi là ProblemDetail (RFC 9457): detail, code, fields, traceId. */
export class ApiError extends Error {
  constructor(status, body) {
    const detail = body?.detail || 'Something went wrong, please try again';
    // Lỗi server: kèm mã tra cứu để người dùng gửi khi báo lỗi
    super(status >= 500 && body?.traceId ? `${detail} (ref: ${body.traceId})` : detail);
    this.status = status;
    this.code = body?.code;
    this.fields = body?.fields || {};
    this.traceId = body?.traceId;
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
 * Refresh rotate cookie; navigator.locks tránh hai tab refresh cùng lúc.
 * Tab vào sau thấy access đã đổi (BroadcastChannel) thì bỏ qua.
 */
async function refreshTokens(failedAccessToken) {
  const run = async () => {
    if (failedAccessToken && tokenStore.access && tokenStore.access !== failedAccessToken) {
      return true;
    }
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { Accept: 'application/json' },
    });
    if (!res.ok) {
      tokenStore.clear();
      return false;
    }
    tokenStore.set(await res.json());
    return true;
  };
  return navigator.locks ? navigator.locks.request('todo-auth-refresh', run) : run();
}

/** Khôi phục phiên sau F5 khi cookie refresh còn hợp lệ. */
export async function tryRestoreSession() {
  if (tokenStore.access) return true;
  return refreshTokens(null);
}

export async function api(path, options = {}) {
  const auth = options.auth !== false;
  const usedAccessToken = tokenStore.access;
  let res = await send(path, options);

  if (res.status === 401 && auth) {
    if (await refreshTokens(usedAccessToken)) {
      res = await send(path, options);
    } else if (usedAccessToken) {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
  }
  return parse(res);
}

/** Multipart upload (không set Content-Type để browser gắn boundary). */
export async function apiUpload(path, formData) {
  const usedAccessToken = tokenStore.access;
  const headers = { Accept: 'application/json' };
  if (tokenStore.access) headers.Authorization = `Bearer ${tokenStore.access}`;

  let res = await fetch(`/api${path}`, {
    method: 'POST',
    headers,
    body: formData,
  });

  if (res.status === 401 && usedAccessToken) {
    if (await refreshTokens(usedAccessToken)) {
      if (tokenStore.access) headers.Authorization = `Bearer ${tokenStore.access}`;
      res = await fetch(`/api${path}`, { method: 'POST', headers, body: formData });
    } else {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
  }
  return parse(res);
}
