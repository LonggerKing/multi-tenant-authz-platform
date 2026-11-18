<template>
  <el-sub-menu
    v-if="node.children && node.children.length"
    :index="String(node.path || node.id)"
  >
    <template #title>
      <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
      <span>{{ node.name }}</span>
    </template>
    <SidebarItem v-for="child in node.children" :key="child.id" :node="child" />
  </el-sub-menu>

  <el-menu-item v-else :index="node.path">
    <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
    <template #title>{{ node.name }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
defineOptions({ name: "SidebarItem" });
defineProps<{ node: any }>();
</script>
