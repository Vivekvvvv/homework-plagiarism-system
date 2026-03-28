import http from "./http";

export const listNotificationsApi = (params?: { status?: number; limit?: number }) =>
  http.get("/notifications", { params });

export const markNotificationsReadApi = (payload: { all: boolean; ids?: number[] }) =>
  http.post("/notifications/read", payload);

export const broadcastNotificationApi = (payload: {
  title: string;
  content?: string;
  level?: string;
  target?: string;
}) => http.post("/notifications/broadcast", payload);

export const listAuditLogsApi = (params?: { actorUsername?: string; action?: string; limit?: number }) =>
  http.get("/audit/logs", { params });
