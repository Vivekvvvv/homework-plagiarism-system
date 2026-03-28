import { defineStore } from "pinia";
import { loginApi, meApi } from "../api/modules";

type UserInfo = {
  id: number;
  username: string;
  realName: string;
  email: string;
  role?: string;
};

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    user: null as UserInfo | null,
  }),
  actions: {
    async login(username: string, password: string) {
      const res = await loginApi({ username, password });
      this.token = res.data.token;
      localStorage.setItem("token", this.token);
      await this.fetchMe();
    },
    async fetchMe() {
      const res = await meApi();
      const data = res.data;
      if (data?.role) data.role = data.role.toLowerCase();
      this.user = data;
    },
    logout() {
      this.token = "";
      this.user = null;
      localStorage.removeItem("token");
    },
  },
});
