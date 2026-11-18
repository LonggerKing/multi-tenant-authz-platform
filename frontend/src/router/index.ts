import { createRouter, createWebHashHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import AdminLayout from "@/layout/AdminLayout.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/login",
      component: () => import("@/views/LoginView.vue"),
      meta: { public: true },
    },
    {
      path: "/",
      component: AdminLayout,
      redirect: "/dashboard",
      children: [
        {
          path: "dashboard",
          component: () => import("@/views/DashboardView.vue"),
          meta: { title: "工作台" },
        },
        {
          path: "system/users",
          component: () => import("@/views/UserView.vue"),
          meta: { title: "用户管理" },
        },
        {
          path: "system/roles",
          component: () => import("@/views/RoleView.vue"),
          meta: { title: "角色权限" },
        },
        {
          path: "system/menus",
          component: () => import("@/views/MenuView.vue"),
          meta: { title: "菜单资源" },
        },
        {
          path: "system/depts",
          component: () => import("@/views/DeptView.vue"),
          meta: { title: "组织架构" },
        },
        {
          path: "system/abac",
          component: () => import("@/views/AbacView.vue"),
          meta: { title: "ABAC 策略" },
        },
        {
          path: "system/logs",
          component: () => import("@/views/LogView.vue"),
          meta: { title: "登录审计" },
        },
        {
          path: "platform/tenants",
          component: () => import("@/views/TenantView.vue"),
          meta: { title: "租户管理" },
        },
        {
          path: "demo/orders",
          component: () => import("@/views/OrderView.vue"),
          meta: { title: "订单数据权限" },
        },
      ],
    },
    {
      path: "/:pathMatch(.*)*",
      component: () => import("@/views/NotFoundView.vue"),
      meta: { public: true },
    },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.meta.public) {
    return true;
  }
  if (!auth.accessToken) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (!auth.loaded) {
    try {
      await auth.fetchInfo();
    } catch {
      auth.reset();
      return { path: "/login" };
    }
  }
  return true;
});

export default router;
