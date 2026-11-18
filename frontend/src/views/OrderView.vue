<template>
  <div class="pp-page">
    <div class="pp-toolbar">
      <div>
        <h2 class="pp-title">订单数据权限</h2>
        <p class="pp-sub">
          同一张表、同一段业务代码，不同角色看到不同行 —— 条件由拦截器自动注入
        </p>
      </div>
      <div class="spacer"></div>
      <el-button @click="load">刷新</el-button>
    </div>

    <section class="scope-banner pp-card">
      <div class="scope-main">
        <span class="pp-chip" :class="{ warn: !scope?.all }">
          {{
            scope?.all
              ? "全部数据"
              : scope?.selfOnly
                ? "仅本人数据"
                : "部门范围"
          }}
        </span>
        <div class="scope-detail">
          <span
            >可见部门：{{
              scope?.deptIds?.length ? scope.deptIds.join("、") : "—"
            }}</span
          >
          <span>包含本人：{{ scope?.selfOnly ? "是" : "否" }}</span>
          <span>共 {{ total }} 条可见记录</span>
        </div>
      </div>
      <div class="scope-sql">
        <small>自动注入的 SQL 片段</small>
        <code>{{ scope?.sqlCondition || "无（不受限）" }}</code>
      </div>
    </section>

    <div class="pp-card" style="margin-top: 16px">
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="180">
          <template #default="{ row }">
            <span class="pp-mono">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="customer" label="客户" min-width="140" />
        <el-table-column prop="amount" label="金额" width="140" align="right">
          <template #default="{ row }"
            >¥ {{ Number(row.amount).toLocaleString() }}</template
          >
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)" effect="light">{{
              row.status
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="deptId"
          label="归属部门 ID"
          width="130"
          align="center"
        />
        <el-table-column
          prop="createBy"
          label="创建人 ID"
          width="130"
          align="center"
        />
        <el-table-column prop="createTime" label="创建时间" width="180" />
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
import { orderApi } from "@/api";

const loading = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const scope = ref<any>(null);
const query = reactive({ pageNum: 1, pageSize: 10 });

onMounted(load);

function statusTag(status: string) {
  const map: Record<string, string> = {
    PAID: "success",
    PENDING: "warning",
    CANCELLED: "info",
  };
  return (map[status] || "info") as any;
}

async function load() {
  loading.value = true;
  try {
    const data: any = await orderApi.page(query);
    rows.value = data.records;
    total.value = data.total;
    scope.value = await orderApi.scope();
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.scope-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 22px;
  background:
    radial-gradient(
      500px 200px at 90% 0%,
      rgba(15, 157, 118, 0.1),
      transparent 70%
    ),
    #fff;
}

.scope-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.scope-detail {
  display: flex;
  gap: 18px;
  font-size: 12.5px;
  color: #6b7688;
}

.scope-sql {
  display: grid;
  gap: 5px;
  text-align: right;
}

.scope-sql small {
  font-size: 10.5px;
  color: #98a2b3;
  letter-spacing: 0.04em;
}

.scope-sql code {
  padding: 6px 10px;
  border-radius: 8px;
  background: #0f1b2d;
  color: #6ee7b7;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11.5px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}

@media (max-width: 900px) {
  .scope-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .scope-sql {
    text-align: left;
  }
}
</style>
