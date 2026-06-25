import { api } from "./client";
import type {
  AuthUser,
  LoginRequest,
  LoginResponse,
  Role,
  UserPayload,
} from "./types";

// --- Authentication ---
export const authApi = {
  login: (d: LoginRequest) => api.post<LoginResponse>("/auth/login", d).then((r) => r.data),
  me: () => api.get<AuthUser>("/auth/me").then((r) => r.data),
};

// --- Users (admin) ---
export const userApi = {
  list: () => api.get<AuthUser[]>("/users").then((r) => r.data),
  get: (id: string) => api.get<AuthUser>(`/users/${id}`).then((r) => r.data),
  create: (d: UserPayload) => api.post<AuthUser>("/users", d).then((r) => r.data),
  update: (id: string, d: UserPayload) => api.put<AuthUser>(`/users/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/users/${id}`).then(() => undefined),
};

// --- Roles (admin) ---
export const roleApi = {
  list: () => api.get<Role[]>("/roles").then((r) => r.data),
  permissions: () => api.get<string[]>("/roles/permissions").then((r) => r.data),
  get: (id: string) => api.get<Role>(`/roles/${id}`).then((r) => r.data),
  create: (d: Role) => api.post<Role>("/roles", d).then((r) => r.data),
  update: (id: string, d: Role) => api.put<Role>(`/roles/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/roles/${id}`).then(() => undefined),
};
