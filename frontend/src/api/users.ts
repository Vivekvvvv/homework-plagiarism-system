import http from "./http";

export const listUsersApi = () => http.get("/users");
export const toggleUserStatusApi = (id: number, status: number) =>
  http.patch(`/users/${id}/status`, null, { params: { status } });
