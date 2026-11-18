<template>
  <div class="pp-page">
    <div class="pp-toolbar">
      <div>
        <h2 class="pp-title">组织架构</h2>
        <p class="pp-sub">
          部门物化路径（path）支撑“本部门及下级”数据范围的子树查询
        </p>
      </div>
      <div class="spacer"></div>
      <el-button @click="load">刷新</el-button>
      <el-button
        v-perm="'system:dept:create'"
        type="primary"
        @click="openCreate(0)"
        >新增部门</el-button
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
        <el-table-column prop="name" label="部门名称" min-width="220" />
        <el-table-column prop="path" label="物化路径" min-width="200">
          <template #default="{ row }">
            <span class="pp-mono">{{ row.path }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="leader" label="负责人" width="130">
          <template #default="{ row }">{{ row.leader || "—" }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">{{
            row.status === 1 ? "启用" : "停用"
          }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              v-perm="'system:dept:create'"
              @click="openCreate(row.id)"
              >新增下级</el-button
            >
            <el-button
              link
              type="primary"
              v-perm="'system:dept:update'"
              @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button
              link
              type="danger"
              v-perm="'system:dept:delete'"
              @click="remove(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑部门' : '新增部门'"
      width="480px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
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
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { deptApi } from "@/api";

const loading = ref(false);
const saving = ref(false);
const tree = ref<any[]>([]);
const dialogVisible = ref(false);
const formRef = ref();

const form = reactive<any>({
  id: null,
  parentId: 0,
  name: "",
  leader: "",
  sort: 0,
  status: 1,
});

const rules = {
  name: [{ required: true, message: "请输入部门名称", trigger: "blur" }],
};

const parentOptions = computed(() => [
  { id: 0, name: "顶级部门", children: tree.value },
]);

onMounted(load);

async function load() {
  loading.value = true;
  try {
    tree.value = await deptApi.tree();
  } finally {
    loading.value = false;
  }
}

function openCreate(parentId: number) {
  Object.assign(form, {
    id: null,
    parentId,
    name: "",
    leader: "",
    sort: 0,
    status: 1,
  });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId || 0,
    name: row.name,
    leader: row.leader || "",
    sort: row.sort || 0,
    status: row.status ?? 1,
  });
  dialogVisible.value = true;
}

async function save() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (form.id) {
        await deptApi.update(form.id, form);
      } else {
        await deptApi.create(form);
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
  await ElMessageBox.confirm(`确认删除部门「${row.name}」？`, "危险操作", {
    type: "warning",
  });
  await deptApi.remove(row.id);
  ElMessage.success("已删除");
  await load();
}
</script>
