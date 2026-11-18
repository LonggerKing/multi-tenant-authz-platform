<template>
  <div class="pp-page">
    <div class="pp-toolbar">
      <div>
        <h2 class="pp-title">ABAC 条件策略</h2>
        <p class="pp-sub">
          在 RBAC 放行之后，用属性表达式（时段 / IP / 部门）动态收敛访问条件
        </p>
      </div>
      <div class="spacer"></div>
      <el-button @click="load">刷新</el-button>
      <el-button
        v-perm="'system:abac:create'"
        type="primary"
        @click="openCreate"
        >新增策略</el-button
      >
    </div>

    <div class="pp-card">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column prop="name" label="策略名称" min-width="170" />
        <el-table-column
          prop="resourcePattern"
          label="资源模式"
          min-width="190"
        >
          <template #default="{ row }">
            <span class="pp-mono">{{ row.resourcePattern }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="httpMethod" label="方法" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{
              row.httpMethod || "*"
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="conditionExpr"
          label="条件表达式"
          min-width="240"
        >
          <template #default="{ row }">
            <code class="expr">{{ row.conditionExpr || "true" }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="effect" label="效果" width="90">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.effect === 'deny' ? 'danger' : 'success'"
              effect="light"
            >
              {{ row.effect }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="priority"
          label="优先级"
          width="90"
          align="center"
        />
        <el-table-column label="启用" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val: boolean) => toggle(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              v-perm="'system:abac:update'"
              @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button
              link
              type="danger"
              v-perm="'system:abac:delete'"
              @click="remove(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑策略' : '新增策略'"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="104px">
        <el-form-item label="策略名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="资源模式" prop="resourcePattern">
          <el-input
            v-model="form.resourcePattern"
            placeholder="权限点编码（demo:order:list）或接口路径（/api/demo/orders/*）"
          />
        </el-form-item>
        <el-form-item label="HTTP 方法">
          <el-select v-model="form.httpMethod" style="width: 100%">
            <el-option label="全部" value="*" />
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件表达式" prop="conditionExpr">
          <el-input
            v-model="form.conditionExpr"
            type="textarea"
            :rows="3"
            placeholder="hour >= 9 && hour < 19"
          />
          <div class="templates">
            <button type="button" @click="applyTpl('hour >= 9 && hour < 19')">
              工作时间 9-19 点
            </button>
            <button
              type="button"
              @click="applyTpl('ip != null && ip.startsWith(\'10.\')')"
            >
              仅内网 10.x
            </button>
            <button
              type="button"
              @click="applyTpl('hasRole(\'TENANT_ADMIN\')')"
            >
              仅租户管理员
            </button>
            <button type="button" @click="applyTpl('deptId != null')">
              需归属部门
            </button>
            <button type="button" @click="applyTpl('true')">始终满足</button>
          </div>
          <p class="expr-tip">
            可用变量：userId / username / tenantId / tenantCode / deptId / roles
            / ip / hour / now
          </p>
        </el-form-item>
        <el-form-item label="效果">
          <el-radio-group v-model="form.effect">
            <el-radio-button label="allow">允许</el-radio-button>
            <el-radio-button label="deny">拒绝</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="1" :max="999" />
          <span class="expr-tip" style="margin-left: 8px">数值越小越优先</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save"
          >确定</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { abacApi } from "@/api";

const loading = ref(false);
const saving = ref(false);
const rows = ref<any[]>([]);
const dialogVisible = ref(false);
const formRef = ref();

const form = reactive<any>({
  id: null,
  name: "",
  resourcePattern: "",
  httpMethod: "*",
  conditionExpr: "true",
  effect: "allow",
  status: 1,
  priority: 100,
  remark: "",
});

const rules = {
  name: [{ required: true, message: "请输入策略名称", trigger: "blur" }],
  resourcePattern: [
    { required: true, message: "请输入资源模式", trigger: "blur" },
  ],
  conditionExpr: [
    { required: true, message: "请输入条件表达式", trigger: "blur" },
  ],
};

onMounted(load);

async function load() {
  loading.value = true;
  try {
    rows.value = await abacApi.list();
  } finally {
    loading.value = false;
  }
}

function applyTpl(expr: string) {
  form.conditionExpr = expr;
}

function openCreate() {
  Object.assign(form, {
    id: null,
    name: "",
    resourcePattern: "",
    httpMethod: "*",
    conditionExpr: "true",
    effect: "allow",
    status: 1,
    priority: 100,
    remark: "",
  });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function save() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (form.id) {
        await abacApi.update(form.id, form);
      } else {
        await abacApi.create(form);
      }
      ElMessage.success("保存成功，策略已即时生效");
      dialogVisible.value = false;
      await load();
    } finally {
      saving.value = false;
    }
  });
}

async function toggle(row: any, enabled: boolean) {
  await abacApi.update(row.id, { status: enabled ? 1 : 0 });
  ElMessage.success(enabled ? "策略已启用" : "策略已停用");
  await load();
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除策略「${row.name}」？`, "危险操作", {
    type: "warning",
  });
  await abacApi.remove(row.id);
  ElMessage.success("已删除");
  await load();
}
</script>

<style scoped>
.expr {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 6px;
  background: #0f1b2d;
  color: #6ee7b7;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11.5px;
}

.templates {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.templates button {
  padding: 4px 9px;
  border-radius: 7px;
  border: 1px solid var(--line);
  background: #f8fafc;
  font-size: 11px;
  color: #55606f;
  cursor: pointer;
}

.templates button:hover {
  border-color: var(--accent);
  color: var(--accent-dark);
}

.expr-tip {
  margin: 6px 0 0;
  font-size: 11px;
  color: #98a2b3;
  line-height: 1.6;
}
</style>
