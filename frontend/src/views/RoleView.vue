<template>
  <div class="pp-page">
    <div class="pp-toolbar">
      <div>
        <h2 class="pp-title">角色权限</h2>
        <p class="pp-sub">
          角色 = 资源授权（Casbin 策略）+ 数据范围（行级过滤）
        </p>
      </div>
      <div class="spacer"></div>
      <el-input
        v-model="query.keyword"
        placeholder="搜索角色编码 / 名称"
        clearable
        style="width: 220px"
        @keyup.enter="load"
      />
      <el-button @click="load">查询</el-button>
      <el-button
        v-perm="'system:role:create'"
        type="primary"
        @click="openCreate"
        >新增角色</el-button
      >
    </div>

    <div class="pp-card">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column prop="code" label="角色编码" width="170">
          <template #default="{ row }">
            <span class="pp-mono">{{ row.code }}</span>
            <el-tag
              v-if="row.builtin"
              size="small"
              type="info"
              effect="plain"
              class="builtin"
              >内置</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column label="数据范围" width="180">
          <template #default="{ row }">
            <el-tag
              :type="
                row.dataScope === 'ALL'
                  ? 'danger'
                  : row.dataScope === 'SELF'
                    ? 'info'
                    : 'success'
              "
              effect="light"
            >
              {{ scopeLabel(row.dataScope) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="200">
          <template #default="{ row }">{{ row.remark || "—" }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span :class="['dot', row.status === 1 ? 'ok' : 'off']"></span>
            {{ row.status === 1 ? "启用" : "停用" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              v-perm="'system:role:update'"
              @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button
              link
              type="primary"
              v-perm="'system:role:assign'"
              @click="openAssign(row)"
              >授权</el-button
            >
            <el-button
              link
              type="danger"
              v-perm="'system:role:delete'"
              @click="remove(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑角色' : '新增角色'"
      width="560px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="角色编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="!!form.id"
            placeholder="大写字母开头，如 OPS_MANAGER"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" value="ALL" />
            <el-option label="本部门" value="DEPT" />
            <el-option label="本部门及下级" value="DEPT_AND_CHILD" />
            <el-option label="仅本人" value="SELF" />
            <el-option label="自定义部门" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.dataScope === 'CUSTOM'" label="自定义部门">
          <el-tree-select
            v-model="form.customDeptIds"
            :data="deptTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            multiple
            show-checkbox
            placeholder="选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="说明">
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

    <el-dialog
      v-model="assignVisible"
      :title="`授权菜单与资源 · ${assignRole?.name || ''}`"
      width="620px"
    >
      <div class="assign-tip">
        <el-icon><InfoFilled /></el-icon>
        勾选后保存会即时重建该角色的 Casbin 策略，并清理全部权限缓存。
      </div>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
        class="menu-tree"
      >
        <template #default="{ data }">
          <span class="tree-node">
            {{ data.name }}
            <span class="tree-code">{{ data.code }}</span>
            <el-tag size="small" effect="plain" class="tree-type">{{
              typeLabel(data.type)
            }}</el-tag>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssign"
          >保存授权</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { deptApi, menuApi, roleApi } from "@/api";

const loading = ref(false);
const saving = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const deptTree = ref<any[]>([]);
const menuTree = ref<any[]>([]);
const dialogVisible = ref(false);
const assignVisible = ref(false);
const assignRole = ref<any>(null);
const formRef = ref();
const menuTreeRef = ref();

const query = reactive({ pageNum: 1, pageSize: 10, keyword: "" });

const form = reactive<any>({
  id: null,
  code: "",
  name: "",
  dataScope: "SELF",
  customDeptIds: [],
  status: 1,
  remark: "",
});

const rules = {
  code: [
    { required: true, message: "请输入角色编码", trigger: "blur" },
    {
      pattern: /^[A-Z][A-Z0-9_]{1,31}$/,
      message: "大写字母开头，2-32 位",
      trigger: "blur",
    },
  ],
  name: [{ required: true, message: "请输入角色名称", trigger: "blur" }],
};

onMounted(async () => {
  await Promise.all([load(), loadDepts(), loadMenus()]);
});

function scopeLabel(scope: string) {
  const map: Record<string, string> = {
    ALL: "全部数据",
    DEPT: "本部门",
    DEPT_AND_CHILD: "本部门及下级",
    SELF: "仅本人",
    CUSTOM: "自定义部门",
  };
  return map[scope] || scope;
}

function typeLabel(type: string) {
  const map: Record<string, string> = {
    DIR: "目录",
    MENU: "页面",
    BUTTON: "按钮",
    API: "接口",
  };
  return map[type] || type;
}

async function load() {
  loading.value = true;
  try {
    const data: any = await roleApi.page(query);
    rows.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

async function loadDepts() {
  deptTree.value = await deptApi.tree();
}

async function loadMenus() {
  menuTree.value = await menuApi.tree();
}

function openCreate() {
  Object.assign(form, {
    id: null,
    code: "",
    name: "",
    dataScope: "SELF",
    customDeptIds: [],
    status: 1,
    remark: "",
  });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id,
    code: row.code,
    name: row.name,
    dataScope: row.dataScope,
    customDeptIds: [...(row.customDeptIds || [])],
    status: row.status,
    remark: row.remark,
  });
  dialogVisible.value = true;
}

async function save() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (form.id) {
        await roleApi.update(form.id, form);
      } else {
        await roleApi.create(form);
      }
      ElMessage.success("保存成功");
      dialogVisible.value = false;
      await load();
    } finally {
      saving.value = false;
    }
  });
}

async function openAssign(row: any) {
  assignRole.value = row;
  assignVisible.value = true;
  const menuIds: number[] = await roleApi.menuIds(row.id);
  await nextTick();
  menuTreeRef.value?.setCheckedKeys(menuIds || []);
}

async function saveAssign() {
  const checked = menuTreeRef.value?.getCheckedKeys() || [];
  const half = menuTreeRef.value?.getHalfCheckedKeys() || [];
  const ids = Array.from(new Set([...checked, ...half])) as number[];
  saving.value = true;
  try {
    await roleApi.assignMenus(assignRole.value.id, ids);
    ElMessage.success("授权已更新，策略即时生效");
    assignVisible.value = false;
  } finally {
    saving.value = false;
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, "危险操作", {
    type: "warning",
  });
  await roleApi.remove(row.id);
  ElMessage.success("已删除");
  await load();
}
</script>

<style scoped>
.builtin {
  margin-left: 6px;
}

.dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
}

.dot.ok {
  background: #0f9d76;
}

.dot.off {
  background: #c0c8d4;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}

.assign-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 9px;
  background: var(--accent-soft);
  color: var(--accent-dark);
  font-size: 12px;
  margin-bottom: 14px;
}

.menu-tree {
  max-height: 420px;
  overflow: auto;
  padding: 6px;
  border: 1px solid var(--line);
  border-radius: 10px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-code {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11px;
  color: #98a2b3;
}

.tree-type {
  transform: scale(0.86);
  transform-origin: left center;
}
</style>
