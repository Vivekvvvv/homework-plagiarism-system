const viteEnv = typeof import.meta !== "undefined" ? import.meta.env : undefined;

export const API_BASE_URL =
  viteEnv?.VITE_API_BASE_URL?.trim() ||
  (viteEnv?.PROD ? "/api/v1" : "http://localhost:8081/api/v1");

export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

export function buildAuthHeaders(token?: string | null): Record<string, string> {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export function getStoredToken(storage: Pick<Storage, "getItem"> = localStorage): string {
  return storage.getItem("token") || "";
}
