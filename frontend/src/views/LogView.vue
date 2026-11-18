<template>
  <div class="pp-page">
    <div class="pp-toolbar">
      <div>
        <h2 class="pp-title">登录审计</h2>
        <p class="pp-sub">
          记录每次登录的租户、IP、终端与结果，支撑安全审计与异常排查
        </p>
      </div>
      <div class="spacer"></div>
      <el-button @click="load">刷新</el-button>
    </div>

    <div class="pp-card">
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="username" label="账号" width="150" />
        <el-table-column prop="ip" label="来源 IP" width="180">
          <template #default="{ row }">
            <span class="pp-mono">{{ row.ip }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="success" label="结果" width="100">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.success ? 'success' : 'danger'"
              effect="light"
            >
              {{ row.success ? "成功" : "失败" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="150" />
        <el-table-column
          prop="userAgent"
          label="终端"
          min-width="260"
          show-overflow-tooltip
        />
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { logApi } from "@/api";

const loading = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 15 });

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const data: any = await logApi.page(query);
    rows.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}
</style>
