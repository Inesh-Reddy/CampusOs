const BASE = "/api/v1";

function csrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

export class ApiError extends Error {
  constructor(status, code, message, fieldErrors, data) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors || null;
    this.data = data || null;
  }
}

async function request(method, path, body) {
  const headers = { "Content-Type": "application/json" };
  if (method !== "GET") {
    const token = csrfToken();
    if (token) headers["X-XSRF-TOKEN"] = token;
  }
  const res = await fetch(BASE + path, {
    method,
    headers,
    credentials: "same-origin",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!res.ok) {
    if (res.status === 401 && !location.pathname.endsWith("login.html")) {
      location.href = "login.html";
    }
    const message = (data && data.message) || ("Request failed (" + res.status + ").");
    throw new ApiError(res.status, data && data.code, message, data && data.fieldErrors, data);
  }
  return data;
}

export const api = {
  get: (path) => request("GET", path),
  post: (path, body) => request("POST", path, body === undefined ? {} : body),
  patch: (path, body) => request("PATCH", path, body),
};

export async function fetchMe() {
  return api.get("/auth/me");
}

export function requireAuth() {
  return fetchMe().catch(() => {
    location.href = "login.html";
    throw new Error("Authentication required.");
  });
}
