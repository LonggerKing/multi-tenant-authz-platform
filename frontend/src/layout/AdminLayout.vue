<template>
  <div class="shell">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="mark">P</div>
        <div v-show="!collapsed" class="brand-text">
          <strong>权限中台</strong>
          <span>UNIFIED PERMISSION</span>
        </div>
      </div>

      <el-scrollbar class="nav">
        <el-menu
          :default-active="activePath"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="nav-menu"
          background-color="transparent"
          text-color="#a9b4c6"
          active-text-color="#ffffff"
        >
          <SidebarItem v-for="menu in auth.menus" :key="menu.id" :node="menu" />
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-foot">
        <button
          class="collapse-btn"
          type="button"
          @click="collapsed = !collapsed"
        >
          <el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
          <span v-show="!collapsed">收起导航</span>
        </button>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="headline">
          <h1>{{ currentTitle }}</h1>
          <span class="env">RBAC · ABAC · Multi-Tenant</span>
        </div>
        <div class="actions">
          <span class="pp-chip neutral">
            <el-icon><OfficeBuilding /></el-icon>
            {{ auth.tenantCode }}
          </span>
          <span v-if="auth.isSuperAdmin" class="pp-chip">平台超管</span>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user">
              <span class="avatar">{{ initials }}</span>
              <span class="user-meta">
                <strong>{{ auth.displayName }}</strong>
                <small>{{ auth.roles.join(" / ") || "未分配角色" }}</small>
              </span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  数据范围：{{ auth.dataScope || "-" }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout"
                  >退出登录</el-dropdown-item
                >
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import SidebarItem from "./SidebarItem.vue";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const collapsed = ref(false);

function syncCollapsed() {
  if (window.innerWidth < 1180) {
    collapsed.value = true;
  }
}

onMounted(() => {
  syncCollapsed();
  window.addEventListener("resize", syncCollapsed);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", syncCollapsed);
});

const activePath = computed(() => route.path);
const currentTitle = computed(() => (route.meta.title as string) || "权限中台");
const initials = computed(() =>
  (auth.displayName || "U").slice(0, 1).toUpperCase(),
);

async function onCommand(command: string) {
  if (command === "logout") {
    await auth.logout();
    router.push("/login");
  }
}
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 236px;
  flex: 0 0 236px;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(1200px 500px at 20% -10%, #1d3a52 0%, transparent 60%),
    linear-gradient(180deg, #0e1829 0%, #0a1220 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  transition:
    flex-basis 0.22s ease,
    width 0.22s ease;
}

.sidebar.collapsed {
  width: 76px;
  flex-basis: 76px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 18px 18px;
}

.mark {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 17px;
  color: #04231a;
  background: linear-gradient(135deg, #4ee0b0 0%, #0f9d76 100%);
  box-shadow: 0 8px 18px -8px rgba(15, 157, 118, 0.9);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  color: #eaf1fb;
}

.brand-text strong {
  font-size: 15px;
  letter-spacing: 0.02em;
}

.brand-text span {
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: #6c7c94;
  margin-top: 3px;
}

.nav {
  flex: 1;
  padding: 6px 10px 0;
}

.nav-menu {
  border-right: none;
}

.nav-menu :deep(.el-menu-item),
.nav-menu :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  border-radius: 10px;
  margin: 3px 0;
  font-size: 13.5px;
}

.nav-menu :deep(.el-menu-item:hover),
.nav-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(
    90deg,
    rgba(15, 157, 118, 0.9) 0%,
    rgba(15, 157, 118, 0.35) 100%
  );
  box-shadow: 0 6px 16px -10px rgba(15, 157, 118, 0.9);
}

.nav-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 48px !important;
}

.sidebar-foot {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.collapse-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  padding: 9px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: #90a0b8;
  font-size: 12.5px;
  cursor: pointer;
}

.collapse-btn:hover {
  color: #dbe6f5;
  background: rgba(255, 255, 255, 0.07);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  height: 68px;
  flex: 0 0 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.headline h1 {
  margin: 0;
  font-size: 16.5px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.headline .env {
  font-size: 11px;
  color: #98a2b3;
  letter-spacing: 0.12em;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  padding: 5px 8px 5px 5px;
  border-radius: 999px;
  transition: background 0.2s ease;
}

.user:hover {
  background: #f1f4f9;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 13px;
  color: #ffffff;
  background: linear-gradient(135deg, #1f3b5c, #0f9d76);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-meta strong {
  font-size: 13px;
  font-weight: 600;
}

.user-meta small {
  font-size: 10.5px;
  color: var(--muted);
}

.content {
  flex: 1;
  overflow: auto;
}

@media (max-width: 1180px) {
  .sidebar {
    width: 76px;
    flex-basis: 76px;
  }

  .brand-text,
  .collapse-btn span {
    display: none;
  }

  .topbar {
    padding: 0 18px;
  }

  .headline .env {
    display: none;
  }
}
</style>
