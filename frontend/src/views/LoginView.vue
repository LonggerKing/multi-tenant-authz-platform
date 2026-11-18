<template>
  <div class="login">
    <section class="brand-panel">
      <div class="grain"></div>
      <div class="brand-top">
        <div class="mark">P</div>
        <span>UNIFIED PERMISSION PLATFORM</span>
      </div>

      <div class="brand-copy">
        <h1>一个中台，<br />统一所有租户与权限。</h1>
        <p>
          多租户隔离、统一认证、Casbin RBAC + ABAC
          动态策略、行列级数据权限，一次接入，全域生效。
        </p>
      </div>

      <div class="metrics">
        <div class="metric">
          <strong>20+</strong>
          <span>微服务接入</span>
        </div>
        <div class="metric">
          <strong>&lt;10ms</strong>
          <span>鉴权耗时</span>
        </div>
        <div class="metric">
          <strong>500+</strong>
          <span>企业租户</span>
        </div>
      </div>

      <div class="feature-list">
        <div class="feature">
          <el-icon><Grid /></el-icon>
          <div>
            <strong>混合隔离</strong>
            <span>共享表 + tenant_id / 独立 Schema 可插拔</span>
          </div>
        </div>
        <div class="feature">
          <el-icon><Lock /></el-icon>
          <div>
            <strong>两级权限模型</strong>
            <span>RBAC 决定能否访问，ABAC 约束访问条件</span>
          </div>
        </div>
        <div class="feature">
          <el-icon><Connection /></el-icon>
          <div>
            <strong>多级缓存</strong>
            <span>Caffeine 本地 + Redis 共享，变更即时失效</span>
          </div>
        </div>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-wrap">
        <h2>登录控制台</h2>
        <p class="hint">使用租户编码 + 账号登录，不同租户数据完全隔离。</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="submit"
        >
          <el-form-item label="租户编码" prop="tenantCode">
            <el-input
              v-model="form.tenantCode"
              size="large"
              placeholder="如 acme / platform"
            >
              <template #prefix
                ><el-icon><OfficeBuilding /></el-icon
              ></template>
            </el-input>
          </el-form-item>
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="用户名">
              <template #prefix
                ><el-icon><User /></el-icon
              ></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              placeholder="密码"
            >
              <template #prefix
                ><el-icon><Key /></el-icon
              ></template>
            </el-input>
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit"
            :loading="loading"
            @click="submit"
          >
            进入控制台
          </el-button>
        </el-form>

        <div class="accounts">
          <span class="accounts-label">演示账号（密码均为 admin123）</span>
          <div class="account-list">
            <button
              v-for="account in demoAccounts"
              :key="account.username + account.tenantCode"
              type="button"
              class="account"
              @click="fill(account)"
            >
              <strong>{{ account.label }}</strong>
              <small>{{ account.tenantCode }} / {{ account.username }}</small>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const formRef = ref();
const loading = ref(false);
const form = reactive({
  tenantCode: "acme",
  username: "admin",
  password: "admin123",
});

const rules = {
  tenantCode: [{ required: true, message: "请输入租户编码", trigger: "blur" }],
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

const demoAccounts = [
  { label: "租户管理员（全部数据）", tenantCode: "acme", username: "admin" },
  {
    label: "部门主管（本部门及下级）",
    tenantCode: "acme",
    username: "manager",
  },
  { label: "数据分析师（仅本人）", tenantCode: "acme", username: "analyst" },
  {
    label: "平台超管（跨租户）",
    tenantCode: "platform",
    username: "superadmin",
  },
];

function fill(account: { tenantCode: string; username: string }) {
  form.tenantCode = account.tenantCode;
  form.username = account.username;
  form.password = "admin123";
}

async function submit() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }
    loading.value = true;
    try {
      await auth.login({ ...form });
      ElMessage.success(`欢迎回来，${auth.displayName}`);
      const redirect = (route.query.redirect as string) || "/dashboard";
      router.push(redirect);
    } finally {
      loading.value = false;
    }
  });
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  background: #f4f6f9;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  padding: 44px 54px;
  display: flex;
  flex-direction: column;
  color: #e8effa;
  background:
    radial-gradient(
      900px 460px at 12% 8%,
      rgba(15, 157, 118, 0.32),
      transparent 62%
    ),
    radial-gradient(
      700px 420px at 92% 82%,
      rgba(46, 111, 178, 0.34),
      transparent 62%
    ),
    linear-gradient(160deg, #0b1526 0%, #080f1c 100%);
}

.grain {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(circle at 30% 20%, black, transparent 78%);
  pointer-events: none;
}

.brand-top {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 10px;
  letter-spacing: 0.22em;
  color: #7f90a8;
}

.mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #04231a;
  background: linear-gradient(135deg, #4ee0b0, #0f9d76);
}

.brand-copy {
  position: relative;
  margin-top: 74px;
  max-width: 460px;
}

.brand-copy h1 {
  font-size: 40px;
  line-height: 1.22;
  letter-spacing: -0.02em;
  margin: 0 0 18px;
  font-weight: 680;
}

.brand-copy p {
  margin: 0;
  font-size: 14px;
  line-height: 1.85;
  color: #9aa9bf;
}

.metrics {
  position: relative;
  display: flex;
  gap: 34px;
  margin-top: 42px;
  padding: 20px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric strong {
  font-size: 24px;
  letter-spacing: -0.02em;
  color: #ffffff;
}

.metric span {
  font-size: 11.5px;
  color: #7d8da5;
}

.feature-list {
  position: relative;
  margin-top: auto;
  padding-top: 34px;
  display: grid;
  gap: 16px;
}

.feature {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.feature .el-icon {
  margin-top: 3px;
  color: #3ddbaa;
  font-size: 16px;
}

.feature strong {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #dfe8f5;
}

.feature span {
  font-size: 11.5px;
  color: #7d8da5;
  line-height: 1.6;
}

.form-panel {
  display: grid;
  place-items: center;
  padding: 40px;
}

.form-wrap {
  width: 100%;
  max-width: 396px;
}

.form-wrap h2 {
  margin: 0;
  font-size: 24px;
  letter-spacing: -0.02em;
}

.hint {
  margin: 8px 0 26px;
  color: var(--muted);
  font-size: 12.5px;
}

.submit {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 14px;
  border-radius: 11px;
  box-shadow: 0 12px 24px -14px rgba(15, 157, 118, 0.9);
}

.accounts {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px dashed var(--line);
}

.accounts-label {
  font-size: 11.5px;
  color: #98a2b3;
  letter-spacing: 0.02em;
}

.account-list {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.account {
  text-align: left;
  padding: 9px 11px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: #ffffff;
  cursor: pointer;
  transition: all 0.18s ease;
}

.account:hover {
  border-color: var(--accent);
  box-shadow: 0 8px 18px -12px rgba(15, 157, 118, 0.7);
}

.account strong {
  display: block;
  font-size: 11.5px;
  font-weight: 600;
  color: #2c3543;
}

.account small {
  display: block;
  margin-top: 2px;
  font-size: 10.5px;
  color: #98a2b3;
}

@media (max-width: 1200px) {
  .brand-panel {
    padding: 36px 38px;
  }

  .brand-copy h1 {
    font-size: 32px;
  }

  .metrics {
    gap: 24px;
  }
}

@media (max-width: 860px) {
  .login {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 28px 20px;
  }
}
</style>
