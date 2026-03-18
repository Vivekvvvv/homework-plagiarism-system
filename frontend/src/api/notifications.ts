import http from "./http";

export const listNotificationsApi = (params?: { status?: number; limit?: number }) =>
  http.get("/notifications", { params });

export const markNotificationsReadApi = (payload: { all: boolean; ids?: number[] }) =>
  http.post("/notifications/read", payload);

export const listAuditLogsApi = (params?: { actorUsername?: string; action?: string; limit?: number }) =>
  http.get("/audit/logs", { params });
