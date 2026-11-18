<template>
  <div class="pp-page dashboard">
    <section class="hero pp-card">
      <div class="hero-copy">
        <span class="pp-chip">当前租户 · {{ auth.tenantCode }}</span>
        <h2>{{ greeting }}，{{ auth.displayName }}</h2>
        <p>
          你正在以
          <strong>{{ auth.roles.join(" / ") || "无角色" }}</strong> 身份访问，
          数据范围 <strong>{{ scopeLabel }}</strong
          >。所有接口的权限判定由 Casbin 策略实时驱动。
        </p>
      </div>
      <div class="hero-badge">
        <div class="badge-label">鉴权链路</div>
        <div class="badge-flow">
          <span>Token</span><i></i><span>租户</span><i></i><span>RBAC</span
          ><i></i><span>ABAC</span>
        </div>
        <div class="badge-foot">本地缓存命中 ≈ 微秒级</div>
      </div>
    </section>

    <section class="stats">
      <article v-for="item in statCards" :key="item.label" class="stat pp-card">
        <span class="stat-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </section>

    <section class="grid">
      <div class="pp-card panel">
        <header class="panel-head">
          <div>
            <h3 class="pp-title">数据权限画像</h3>
            <p class="pp-sub">行级范围由角色推导，SQL 条件由拦截器自动注入</p>
          </div>
          <el-tag
            v-if="scope"
            :type="scope.all ? 'success' : 'warning'"
            effect="light"
          >
            {{
              scope.all ? "全部数据" : scope.selfOnly ? "仅本人" : "部门范围"
            }}
          </el-tag>
        </header>

        <div v-if="scope" class="scope-body">
          <div class="scope-row">
            <span>可见部门</span>
            <strong>{{
              scope.deptIds && scope.deptIds.length
                ? scope.deptIds.join("、")
                : "—"
            }}</strong>
          </div>
          <div class="scope-row">
            <span>本人数据</span>
            <strong>{{ scope.selfOnly ? "包含" : "不包含" }}</strong>
          </div>
          <div class="scope-code">
            <span>注入条件</span>
            <code>{{ scope.sqlCondition || "无（不受限）" }}</code>
          </div>
        </div>
        <div v-else class="pp-empty">
          当前角色无业务数据访问权限，或未接入数据权限演示模块
        </div>
      </div>

      <div class="pp-card panel">
        <header class="panel-head">
          <div>
            <h3 class="pp-title">按钮权限点</h3>
            <p class="pp-sub">
              共 {{ auth.permissions.length }} 个权限点，前端通过 v-perm
              指令控制
            </p>
          </div>
        </header>
        <div class="perm-cloud">
          <span v-for="code in auth.permissions" :key="code" class="perm">{{
            code
          }}</span>
          <span v-if="!auth.permissions.length" class="pp-empty"
            >暂无权限点</span
          >
        </div>
      </div>
    </section>

    <section class="grid three">
      <article class="pp-card capability">
        <div class="cap-icon">
          <el-icon><Grid /></el-icon>
        </div>
        <h4>混合租户隔离</h4>
        <p>
          共享表按 tenant_id 自动过滤；大客户可开通独立 Schema，MyBatis
          动态改写表名。
        </p>
      </article>
      <article class="pp-card capability">
        <div class="cap-icon">
          <el-icon><Lock /></el-icon>
        </div>
        <h4>RBAC + ABAC</h4>
        <p>
          角色决定资源访问权，属性条件（时段 / IP / 部门）在放行前二次收敛。
        </p>
      </article>
      <article class="pp-card capability">
        <div class="cap-icon">
          <el-icon><Refresh /></el-icon>
        </div>
        <h4>策略即时生效</h4>
        <p>授权变更后重建策略引擎并清理两级缓存，无需重启或等待过期。</p>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "@/stores/auth";
import { orderApi, roleApi, tenantApi, userApi } from "@/api";

const auth = useAuthStore();
const counts = ref({ users: 0, roles: 0, orders: 0, tenants: 0 });
const scope = ref<any>(null);

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return "夜深了";
  if (hour < 12) return "早上好";
  if (hour < 18) return "下午好";
  return "晚上好";
});

const scopeLabel = computed(() => {
  if (auth.isSuperAdmin) return "全部数据（平台超管）";
  const map: Record<string, string> = {
    ALL: "全部数据",
    DEPT: "本部门",
    DEPT_AND_SELF: "本部门 + 本人",
    SELF: "仅本人",
  };
  return map[auth.dataScope] || auth.dataScope || "—";
});

const statCards = computed(() => [
  {
    label: "租户用户",
    value: counts.value.users || "—",
    hint: "当前租户下账号数",
  },
  { label: "角色数量", value: counts.value.roles || "—", hint: "含内置角色" },
  {
    label: "可见订单",
    value: counts.value.orders || "—",
    hint: "受行级数据权限约束",
  },
  {
    label: "平台租户",
    value: counts.value.tenants || "—",
    hint: "仅平台超管可见",
  },
]);

onMounted(async () => {
  await Promise.allSettled([
    loadUsers(),
    loadRoles(),
    loadOrders(),
    loadTenants(),
  ]);
});

async function loadUsers() {
  if (!auth.has("system:user:list")) return;
  const data: any = await userApi.page({ pageNum: 1, pageSize: 1 });
  counts.value.users = data.total;
}

async function loadRoles() {
  if (!auth.has("system:role:list")) return;
  const data: any = await roleApi.page({ pageNum: 1, pageSize: 1 });
  counts.value.roles = data.total;
}

async function loadOrders() {
  if (!auth.has("demo:order:list")) return;
  const data: any = await orderApi.page({ pageNum: 1, pageSize: 1 });
  counts.value.orders = data.total;
  scope.value = await orderApi.scope();
}

async function loadTenants() {
  if (!auth.isSuperAdmin) return;
  const data: any = await tenantApi.page({ pageNum: 1, pageSize: 1 });
  counts.value.tenants = data.total;
}
</script>

<style scoped>
.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 30px;
  background:
    radial-gradient(
      600px 240px at 88% 0%,
      rgba(15, 157, 118, 0.12),
      transparent 70%
    ),
    #ffffff;
}

.hero-copy h2 {
  margin: 12px 0 8px;
  font-size: 22px;
  letter-spacing: -0.02em;
}

.hero-copy p {
  margin: 0;
  max-width: 560px;
  color: #5d6879;
  font-size: 13px;
  line-height: 1.8;
}

.hero-copy strong {
  color: #12263f;
}

.hero-badge {
  flex: 0 0 300px;
  align-self: center;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 16px 18px;
  background: #fafbfd;
}

.badge-label {
  font-size: 11px;
  letter-spacing: 0.14em;
  color: #98a2b3;
}

.badge-flow {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 10px 0 8px;
  font-size: 11.5px;
  font-weight: 600;
  color: #1c3550;
}

.badge-flow i {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, #cfe3db, #9fd6c4);
}

.badge-foot {
  font-size: 11px;
  color: #7c8798;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.stat {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 12px;
  color: #7c8798;
}

.stat strong {
  font-size: 26px;
  letter-spacing: -0.02em;
}

.stat small {
  font-size: 11px;
  color: #a3adbd;
}

.grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 16px;
  margin-top: 18px;
}

.grid.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.panel {
  padding: 20px 22px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.scope-body {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.scope-row {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--line);
  color: #7c8798;
}

.scope-row strong {
  color: #1c2733;
  font-weight: 600;
}

.scope-code {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: #7c8798;
}

.scope-code code {
  display: block;
  padding: 10px 12px;
  border-radius: 9px;
  background: #0f1b2d;
  color: #6ee7b7;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12px;
  word-break: break-all;
}

.perm-cloud {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  max-height: 190px;
  overflow: auto;
}

.perm {
  padding: 4px 9px;
  border-radius: 7px;
  background: #f2f5f9;
  border: 1px solid #e6ebf2;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11px;
  color: #41505f;
}

.capability {
  padding: 20px 22px;
}

.cap-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: var(--accent-soft);
  color: var(--accent-dark);
  margin-bottom: 12px;
}

.capability h4 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 650;
}

.capability p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.75;
  color: #6b7688;
}

@media (max-width: 1180px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .grid,
  .grid.three {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }

  .hero-badge {
    flex: none;
    width: 100%;
    align-self: stretch;
  }
}
</style>
