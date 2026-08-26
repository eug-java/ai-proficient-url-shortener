import { useAuth } from "./auth";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function apiFetch<T>(
  path: string,
  getToken: () => Promise<string | undefined>,
  init: RequestInit = {}
): Promise<T> {
  const token = await getToken();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `${res.status} ${res.statusText}`);
  }
  if (res.status === 204) return undefined as T;
  const contentType = res.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return (await res.json()) as T;
  }
  return (await res.text()) as T;
}

export function useApi() {
  const { getToken } = useAuth();
  return {
    get: <T,>(path: string) => apiFetch<T>(path, getToken),
    post: <T,>(path: string, body?: unknown) =>
      apiFetch<T>(path, getToken, {
        method: "POST",
        body: body === undefined ? undefined : JSON.stringify(body),
      }),
    patch: <T,>(path: string, body?: unknown) =>
      apiFetch<T>(path, getToken, {
        method: "PATCH",
        body: body === undefined ? undefined : JSON.stringify(body),
      }),
    del: <T,>(path: string) =>
      apiFetch<T>(path, getToken, { method: "DELETE" }),
  };
}

export type Org = {
  id: string;
  name: string;
  slug: string;
  role: string;
};

export type LinkItem = {
  id: string;
  shortCode: string;
  shortUrl: string;
  originalUrl: string;
  title?: string;
  status: string;
  expiresAt?: string;
  createdAt: string;
  totalClicks: number;
  lastAccessedAt?: string;
};

export type AnalyticsSummary = {
  shortCode: string;
  totalClicks: number;
  lastClickedAt?: string;
  uniqueVisitorsApprox: number;
};
