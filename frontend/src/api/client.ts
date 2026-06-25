import axios from "axios";

/**
 * Shared Axios instance. All calls go through the /api proxy to the backend.
 *
 * Authentication: a JWT bearer token (obtained at login) is attached as the
 * Authorization header on every request. The token also carries the tenant, so
 * for normal users the X-Company-Id header is not needed. Platform/super-admins
 * (whose token has no company) may still set a company id to scope their actions.
 */
export const TOKEN_STORAGE_KEY = "hrms.token";
export const COMPANY_STORAGE_KEY = "hrms.companyId";

export function getToken(): string {
  return localStorage.getItem(TOKEN_STORAGE_KEY) ?? "";
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

export function getCompanyId(): string {
  return localStorage.getItem(COMPANY_STORAGE_KEY) ?? "";
}

export function setCompanyId(id: string): void {
  localStorage.setItem(COMPANY_STORAGE_KEY, id);
}

export const api = axios.create({
  baseURL: "/api/v1",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  const companyId = getCompanyId();
  if (companyId) {
    config.headers.set("X-Company-Id", companyId);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url: string = error?.config?.url ?? "";
    // On an expired/invalid session, drop the token and bounce to login -
    // but not for the login call itself (that's just bad credentials).
    if (status === 401 && !url.includes("/auth/login")) {
      clearToken();
      if (window.location.pathname !== "/login") {
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  }
);
