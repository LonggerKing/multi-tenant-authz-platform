<template>
  <div class="pp-page">
    <div class="pp-toolbar">
      <div>
        <h2 class="pp-title">菜单资源</h2>
        <p class="pp-sub">
          目录 / 页面 / 按钮权限点 / 接口资源统一建模，授权即生成 Casbin 策略
        </p>
      </div>
      <div class="spacer"></div>
      <el-button @click="load">刷新</el-button>
      <el-button
        v-perm="'system:menu:create'"
        type="primary"
        @click="openCreate(0)"
        >新增资源</el-button
      >
    </div>

    <div class="pp-card">
      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        style="width: 100%"
      >
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column prop="code" label="权限点编码" min-width="200">
          <template #default="{ row }">
            <span v-if="row.code" class="pp-mono">{{ row.code }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)" effect="light">{{
              typeLabel(row.type)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由 / 接口路径" min-width="180">
          <template #default="{ row }">{{ row.path || "—" }}</template>
        </el-table-column>
        <el-table-column prop="component" label="组件" min-width="140">
          <template #default="{ row }">{{ row.component || "—" }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="visible" label="显示" width="80" align="center">
          <template #default="{ row }">{{
            row.visible === 1 ? "是" : "否"
          }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              v-perm="'system:menu:create'"
              @click="openCreate(row.id)"
              >新增子项</el-button
            >
            <el-button
              link
              type="primary"
              v-perm="'system:menu:update'"
              @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button
              link
              type="danger"
              v-perm="'system:menu:delete'"
              @click="remove(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑资源' : '新增资源'"
      width="560px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="104px">
        <el-form-item label="上级节点">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button label="DIR">目录</el-radio-button>
            <el-radio-button label="MENU">页面</el-radio-button>
            <el-radio-button label="BUTTON">按钮</el-radio-button>
            <el-radio-button label="API">接口</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限点编码">
          <el-input v-model="form.code" placeholder="如 system:user:create" />
        </el-form-item>
        <el-form-item
          v-if="form.type === 'MENU' || form.type === 'API'"
          label="路径"
        >
          <el-input
            v-model="form.path"
            :placeholder="
              form.type === 'API' ? '/api/demo/orders/*' : '/system/users'
            "
          />
        </el-form-item>
        <el-form-item v-if="form.type === 'API'" label="HTTP 方法">
          <el-select v-model="form.httpMethod" style="width: 100%">
            <el-option label="全部" value="*" />
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="组件路径">
          <el-input v-model="form.component" placeholder="system/users" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input
            v-model="form.icon"
            placeholder="Element Plus 图标名，如 User"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="显示">
          <el-switch
            v-model="form.visible"
            :active-value="1"
            :inactive-value="0"
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
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { menuApi } from "@/api";

const loading = ref(false);
const saving = ref(false);
const tree = ref<any[]>([]);
const dialogVisible = ref(false);
const formRef = ref();

const form = reactive<any>({
  id: null,
  parentId: 0,
  name: "",
  code: "",
  type: "MENU",
  path: "",
  component: "",
  icon: "",
  httpMethod: "*",
  sort: 0,
  visible: 1,
});

const rules = {
  name: [{ required: true, message: "请输入名称", trigger: "blur" }],
  type: [{ required: true, message: "请选择类型", trigger: "change" }],
};

const parentOptions = computed(() => [
  { id: 0, name: "顶级节点", children: excludeSelf(tree.value, form.id) },
]);

onMounted(load);

function excludeSelf(nodes: any[], selfId: number | null): any[] {
  return nodes
    .filter((node) => node.id !== selfId)
    .map((node) => ({
      ...node,
      children: node.children ? excludeSelf(node.children, selfId) : [],
    }));
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

function typeTag(type: string) {
  const map: Record<string, string> = {
    DIR: "info",
    MENU: "success",
    BUTTON: "warning",
    API: "danger",
  };
  return (map[type] || "info") as any;
}

async function load() {
  loading.value = true;
  try {
    tree.value = await menuApi.tree();
  } finally {
    loading.value = false;
  }
}

function openCreate(parentId: number) {
  Object.assign(form, {
    id: null,
    parentId,
    name: "",
    code: "",
    type: "MENU",
    path: "",
    component: "",
    icon: "",
    httpMethod: "*",
    sort: 0,
    visible: 1,
  });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId || 0,
    name: row.name,
    code: row.code || "",
    type: row.type,
    path: row.path || "",
    component: row.component || "",
    icon: row.icon || "",
    httpMethod: row.httpMethod || "*",
    sort: row.sort || 0,
    visible: row.visible ?? 1,
  });
  dialogVisible.value = true;
}

async function save() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (form.id) {
        await menuApi.update(form.id, form);
      } else {
        await menuApi.create(form);
      }
      ElMessage.success("保存成功");
      dialogVisible.value = false;
      await load();
    } finally {
      saving.value = false;
    }
  });
}

async function remove(row: any) {
  await ElMessageBox.confirm(
    `确认删除「${row.name}」？删除后关联角色将失去该权限点。`,
    "危险操作",
    {
      type: "warning",
    },
  );
  await menuApi.remove(row.id);
  ElMessage.success("已删除");
  await load();
}
</script>

<style scoped>
.muted {
  color: #a3adbd;
}
</style>
