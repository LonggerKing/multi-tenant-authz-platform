<template>
  <div class="pp-page">
    <div class="pp-toolbar">
      <div>
        <h2 class="pp-title">租户管理</h2>
        <p class="pp-sub">
          共享表（tenant_id）与独立 Schema 两种隔离模式可按租户切换
        </p>
      </div>
      <div class="spacer"></div>
      <el-input
        v-model="query.keyword"
        placeholder="搜索编码 / 名称"
        clearable
        style="width: 220px"
        @keyup.enter="load"
      />
      <el-select
        v-model="query.status"
        placeholder="状态"
        clearable
        style="width: 130px"
      >
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button @click="load">查询</el-button>
      <el-button
        v-perm="'system:tenant:create'"
        type="primary"
        @click="openCreate"
        >新增租户</el-button
      >
    </div>

    <div class="pp-card">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        style="width: 100%"
      >
        <el-table-column prop="code" label="租户编码" width="140">
          <template #default="{ row }">
            <span class="pp-mono">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="租户名称" min-width="160" />
        <el-table-column prop="isolationMode" label="隔离模式" width="150">
          <template #default="{ row }">
            <el-tag
              :type="row.isolationMode === 'SCHEMA' ? 'warning' : 'success'"
              effect="light"
            >
              {{ row.isolationMode === "SCHEMA" ? "独立 Schema" : "共享表" }}
            </el-tag>
            <div v-if="row.schemaName" class="pp-mono schema">
              {{ row.schemaName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="96">
          <template #default="{ row }">
            <span :class="['dot', row.status === 1 ? 'ok' : 'off']"></span>
            {{ row.status === 1 ? "启用" : "停用" }}
          </template>
        </el-table-column>
        <el-table-column
          prop="maxUsers"
          label="用户上限"
          width="100"
          align="center"
        />
        <el-table-column prop="expireAt" label="到期时间" width="170">
          <template #default="{ row }">{{ row.expireAt || "永久" }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              v-perm="'system:tenant:update'"
              @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button
              link
              type="primary"
              v-perm="'system:tenant:update'"
              @click="toggle(row)"
            >
              {{ row.status === 1 ? "停用" : "启用" }}
            </el-button>
            <el-button
              link
              type="danger"
              v-perm="'system:tenant:delete'"
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
      :title="form.id ? '编辑租户' : '新增租户'"
      width="520px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="租户编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="!!form.id"
            placeholder="小写字母开头，如 globex"
          />
        </el-form-item>
        <el-form-item label="租户名称" prop="name">
          <el-input v-model="form.name" placeholder="企业名称" />
        </el-form-item>
        <el-form-item label="隔离模式" prop="isolationMode">
          <el-radio-group v-model="form.isolationMode" :disabled="!!form.id">
            <el-radio-button label="SHARED">共享表</el-radio-button>
            <el-radio-button label="SCHEMA">独立 Schema</el-radio-button>
          </el-radio-group>
          <div class="form-tip">
            独立 Schema 会在创建时自动开通 schema、建表并初始化管理员 admin /
            admin123
          </div>
        </el-form-item>
        <el-form-item label="用户上限">
          <el-input-number v-model="form.maxUsers" :min="1" :max="100000" />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker
            v-model="form.expireAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示永久"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { tenantApi } from "@/api";

const loading = ref(false);
const saving = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
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
  code: "",
  name: "",
  isolationMode: "SHARED",
  maxUsers: 100,
  expireAt: null,
  contactName: "",
  contactPhone: "",
  remark: "",
});

const rules = {
  code: [
    { required: true, message: "请输入租户编码", trigger: "blur" },
    {
      pattern: /^[a-z][a-z0-9_]{2,31}$/,
      message: "小写字母开头，3-32 位",
      trigger: "blur",
    },
  ],
  name: [{ required: true, message: "请输入租户名称", trigger: "blur" }],
};

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const data: any = await tenantApi.page(query);
    rows.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, {
    id: null,
    code: "",
    name: "",
    isolationMode: "SHARED",
    maxUsers: 100,
    expireAt: null,
    contactName: "",
    contactPhone: "",
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
        await tenantApi.update(form.id, form);
      } else {
        await tenantApi.create(form);
      }
      ElMessage.success("保存成功");
      dialogVisible.value = false;
      await load();
    } finally {
      saving.value = false;
    }
  });
}

async function toggle(row: any) {
  await tenantApi.changeStatus(row.id, row.status === 1 ? 0 : 1);
  ElMessage.success("状态已更新");
  await load();
}

async function remove(row: any) {
  await ElMessageBox.confirm(
    `确认删除租户「${row.name}」？该操作不可恢复。`,
    "危险操作",
    { type: "warning" },
  );
  await tenantApi.remove(row.id);
  ElMessage.success("已删除");
  await load();
}
</script>

<style scoped>
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

.schema {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 11px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}

.form-tip {
  font-size: 11.5px;
  color: #98a2b3;
  line-height: 1.6;
  margin-top: 4px;
}
</style>
