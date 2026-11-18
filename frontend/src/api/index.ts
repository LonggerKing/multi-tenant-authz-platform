import http from "./http";

export const authApi = {
  login: (data: { tenantCode: string; username: string; password: string }) =>
    http.post("/auth/login", data) as Promise<any>,
  userInfo: () => http.get("/auth/userinfo") as Promise<any>,
  logout: () => http.post("/auth/logout") as Promise<any>,
};

export const tenantApi = {
  page: (params: any) => http.get("/tenants", { params }) as Promise<any>,
  all: () => http.get("/tenants/all") as Promise<any>,
  create: (data: any) => http.post("/tenants", data) as Promise<any>,
  update: (id: number, data: any) =>
    http.put(`/tenants/${id}`, data) as Promise<any>,
  changeStatus: (id: number, status: number) =>
    http.put(`/tenants/${id}/status`, { status }) as Promise<any>,
  remove: (id: number) => http.delete(`/tenants/${id}`) as Promise<any>,
  current: () => http.get("/tenants/current") as Promise<any>,
};

export const userApi = {
  page: (params: any) => http.get("/system/users", { params }) as Promise<any>,
  detail: (id: number) => http.get(`/system/users/${id}`) as Promise<any>,
  create: (data: any) => http.post("/system/users", data) as Promise<any>,
  update: (id: number, data: any) =>
    http.put(`/system/users/${id}`, data) as Promise<any>,
  remove: (id: number) => http.delete(`/system/users/${id}`) as Promise<any>,
  resetPassword: (id: number, password: string) =>
    http.put(`/system/users/${id}/password`, { password }) as Promise<any>,
  assignRoles: (id: number, ids: number[]) =>
    http.put(`/system/users/${id}/roles`, { ids }) as Promise<any>,
};

export const roleApi = {
  page: (params: any) => http.get("/system/roles", { params }) as Promise<any>,
  all: () => http.get("/system/roles/all") as Promise<any>,
  create: (data: any) => http.post("/system/roles", data) as Promise<any>,
  update: (id: number, data: any) =>
    http.put(`/system/roles/${id}`, data) as Promise<any>,
  remove: (id: number) => http.delete(`/system/roles/${id}`) as Promise<any>,
  menuIds: (id: number) =>
    http.get(`/system/roles/${id}/menus`) as Promise<any>,
  assignMenus: (id: number, ids: number[]) =>
    http.put(`/system/roles/${id}/menus`, { ids }) as Promise<any>,
};

export const menuApi = {
  tree: () => http.get("/system/menus/tree") as Promise<any>,
  list: () => http.get("/system/menus") as Promise<any>,
  create: (data: any) => http.post("/system/menus", data) as Promise<any>,
  update: (id: number, data: any) =>
    http.put(`/system/menus/${id}`, data) as Promise<any>,
  remove: (id: number) => http.delete(`/system/menus/${id}`) as Promise<any>,
};

export const deptApi = {
  tree: () => http.get("/system/depts/tree") as Promise<any>,
  list: () => http.get("/system/depts") as Promise<any>,
  create: (data: any) => http.post("/system/depts", data) as Promise<any>,
  update: (id: number, data: any) =>
    http.put(`/system/depts/${id}`, data) as Promise<any>,
  remove: (id: number) => http.delete(`/system/depts/${id}`) as Promise<any>,
};

export const abacApi = {
  list: () => http.get("/system/abac-policies") as Promise<any>,
  create: (data: any) =>
    http.post("/system/abac-policies", data) as Promise<any>,
  update: (id: number, data: any) =>
    http.put(`/system/abac-policies/${id}`, data) as Promise<any>,
  remove: (id: number) =>
    http.delete(`/system/abac-policies/${id}`) as Promise<any>,
};

export const logApi = {
  page: (params: any) =>
    http.get("/system/login-logs", { params }) as Promise<any>,
};

export const orderApi = {
  page: (params: any) => http.get("/demo/orders", { params }) as Promise<any>,
  scope: () => http.get("/demo/orders/scope") as Promise<any>,
};
