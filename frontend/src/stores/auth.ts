import { defineStore } from "pinia";
import { authApi, tenantApi } from "@/api";
import { REFRESH_KEY, TOKEN_KEY } from "@/api/http";

const TENANT_CODE_KEY = "pp_tenant_code";

interface MenuNode {
  id: number;
  parentId: number;
  name: string;
  code?: string;
  type: string;
  path?: string;
  icon?: string;
  children?: MenuNode[];
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    accessToken: localStorage.getItem(TOKEN_KEY) || "",
    refreshToken: localStorage.getItem(REFRESH_KEY) || "",
    tenantCode: localStorage.getItem(TENANT_CODE_KEY) || "",
    user: null as any,
    roles: [] as string[],
    permissions: [] as string[],
    menus: [] as MenuNode[],
    dataScope: "",
    loaded: false,
  }),

  getters: {
    isSuperAdmin: (state) => !!state.user?.superAdmin,
    displayName: (state) => state.user?.nickname || state.user?.username || "",
  },

  actions: {
    async login(payload: {
      tenantCode: string;
      username: string;
      password: string;
    }) {
      const data: any = await authApi.login(payload);
      this.accessToken = data.accessToken;
      this.refreshToken = data.refreshToken;
      this.tenantCode = data.tenantCode;
      localStorage.setItem(TOKEN_KEY, data.accessToken);
      localStorage.setItem(REFRESH_KEY, data.refreshToken);
      localStorage.setItem(TENANT_CODE_KEY, data.tenantCode || "");
      await this.fetchInfo();
    },

    async fetchInfo() {
      const data: any = await authApi.userInfo();
      this.user = data.user;
      this.roles = data.roles || [];
      this.permissions = data.permissions || [];
      this.menus = data.menus || [];
      this.dataScope = data.dataScope || "";
      if (!this.tenantCode) {
        try {
          const tenant: any = await tenantApi.current();
          this.tenantCode = tenant.code;
          localStorage.setItem(TENANT_CODE_KEY, tenant.code);
        } catch {
          // 忽略：租户信息不影响主流程
        }
      }
      this.loaded = true;
    },

    has(code: string) {
      return this.isSuperAdmin || this.permissions.includes(code);
    },

    async logout() {
      try {
        await authApi.logout();
      } catch {
        // 忽略登出接口异常
      }
      this.reset();
    },

    reset() {
      this.accessToken = "";
      this.refreshToken = "";
      this.tenantCode = "";
      this.user = null;
      this.roles = [];
      this.permissions = [];
      this.menus = [];
      this.loaded = false;
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_KEY);
      localStorage.removeItem(TENANT_CODE_KEY);
    },
  },
});
