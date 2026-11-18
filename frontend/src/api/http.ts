import axios from "axios";
import { ElMessage } from "element-plus";

export const TOKEN_KEY = "pp_access_token";
export const REFRESH_KEY = "pp_refresh_token";

const http = axios.create({
  baseURL: "/api",
  timeout: 20000,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body.code === "number") {
      if (body.code === 0) {
        return body.data;
      }
      ElMessage.error(body.message || "请求失败");
      return Promise.reject(new Error(body.message || "请求失败"));
    }
    return body;
  },
  (error) => {
    const status = error?.response?.status;
    const message = error?.response?.data?.message;
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_KEY);
      if (!location.hash.startsWith("#/login")) {
        location.hash = "#/login";
      }
      ElMessage.error(message || "登录状态已失效，请重新登录");
    } else if (status === 403) {
      ElMessage.error(message || "没有访问权限");
    } else {
      ElMessage.error(message || "网络请求失败");
    }
    return Promise.reject(error);
  },
);

export default http;
