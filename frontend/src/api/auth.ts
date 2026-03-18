import http from "./http";

export type LoginPayload = {
  username: string;
  password: string;
};

export type ChangePasswordPayload = {
  oldPassword: string;
  newPassword: string;
};

export const loginApi = (payload: LoginPayload) => http.post("/auth/login", payload);
export const meApi = () => http.get("/auth/me");
export const changePasswordApi = (payload: ChangePasswordPayload) => http.post("/auth/change-password", payload);
