<template>
  <div class="pp-page">
    <div class="pp-toolbar">
      <div>
        <h2 class="pp-title">用户管理</h2>
        <p class="pp-sub">
          账号归属租户与部门，角色决定其可见的菜单、按钮与数据范围
        </p>
      </div>
      <div class="spacer"></div>
      <el-input
        v-model="query.keyword"
        placeholder="搜索用户名 / 昵称 / 手机号"
        clearable
        style="width: 240px"
        @keyup.enter="load"
      />
      <el-select
        v-model="query.status"
        placeholder="状态"
        clearable
        style="width: 120px"
      >
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button @click="load">查询</el-button>
      <el-button
        v-perm="'system:user:create'"
        type="primary"
        @click="openCreate"
        >新增用户</el-button
      >
    </div>

    <div class="pp-card">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="cell-user">
              <span class="mini-avatar">{{
                (row.nickname || row.username || "U").slice(0, 1)
              }}</span>
              <div>
                <strong>{{ row.nickname || row.username }}</strong>
                <small class="pp-mono">{{ row.username }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="所属部门" width="150">
          <template #default="{ row }">{{ row.deptName || "—" }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="name in row.roleNames"
              :key="name"
              size="small"
              effect="plain"
              class="role-tag"
            >
              {{ name }}
            </el-tag>
            <span v-if="!row.roleNames?.length" class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || "—" }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="96">
          <template #default="{ row }">
            <span :class="['dot', row.status === 1 ? 'ok' : 'off']"></span>
            {{ row.status === 1 ? "启用" : "停用" }}
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最近登录" width="170">
          <template #default="{ row }">{{
            row.lastLoginTime || "从未登录"
          }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              v-perm="'system:user:update'"
              @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button
              link
              type="primary"
              v-perm="'system:user:reset'"
              @click="resetPassword(row)"
              >重置密码</el-button
            >
            <el-button
              link
              type="danger"
              v-perm="'system:user:delete'"
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
          layout="total, prev, pager, next, sizes"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑用户' : '新增用户'"
      width="560px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            :disabled="!!form.id"
            placeholder="登录账号"
          />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="至少 6 位"
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="form.roleIds"
            multiple
            placeholder="选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
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
import { deptApi, roleApi, userApi } from "@/api";

const loading = ref(false);
const saving = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const roles = ref<any[]>([]);
const deptTree = ref<any[]>([]);
const dialogVisible = ref(false);
const formRef = ref();

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  status: undefined as number | undefined,
});

const form = reactive<any>({
  id: null,
  username: "",
  nickname: "",
  password: "",
  deptId: null,
  roleIds: [],
  email: "",
  phone: "",
  status: 1,
});

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,31}$/,
      message: "字母开头，3-32 位",
      trigger: "blur",
    },
  ],
  password: [{ required: true, message: "请输入初始密码", trigger: "blur" }],
};

onMounted(async () => {
  await Promise.all([load(), loadRoles(), loadDepts()]);
});

async function load() {
  loading.value = true;
  try {
    const data: any = await userApi.page(query);
    rows.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

async function loadRoles() {
  roles.value = await roleApi.all();
}

async function loadDepts() {
  deptTree.value = await deptApi.tree();
}

function openCreate() {
  Object.assign(form, {
    id: null,
    username: "",
    nickname: "",
    password: "",
    deptId: null,
    roleIds: [],
    email: "",
    phone: "",
    status: 1,
  });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    deptId: row.deptId,
    roleIds: [...(row.roleIds || [])],
    email: row.email,
    phone: row.phone,
    status: row.status,
  });
  dialogVisible.value = true;
}

async function save() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (form.id) {
        await userApi.update(form.id, {
          nickname: form.nickname,
          deptId: form.deptId,
          email: form.email,
          phone: form.phone,
          status: form.status,
          roleIds: form.roleIds,
        });
      } else {
        await userApi.create(form);
      }
      ElMessage.success("保存成功");
      dialogVisible.value = false;
      await load();
    } finally {
      saving.value = false;
    }
  });
}

async function resetPassword(row: any) {
  const { value } = await ElMessageBox.prompt(
    `为「${row.username}」设置新密码`,
    "重置密码",
    {
      inputType: "password",
      inputPlaceholder: "至少 6 位",
      inputValidator: (val) =>
        val && val.length >= 6 ? true : "密码至少 6 位",
    },
  );
  await userApi.resetPassword(row.id, value);
  ElMessage.success("密码已重置");
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, "危险操作", {
    type: "warning",
  });
  await userApi.remove(row.id);
  ElMessage.success("已删除");
  await load();
}
</script>

<style scoped>
.cell-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mini-avatar {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #0b4a39;
  background: #dcf3ea;
}

.cell-user strong {
  display: block;
  font-size: 13px;
  font-weight: 600;
}

.cell-user small {
  color: #98a2b3;
  font-size: 11px;
}

.role-tag {
  margin-right: 4px;
}

.muted {
  color: #a3adbd;
  font-size: 12px;
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
</style>
