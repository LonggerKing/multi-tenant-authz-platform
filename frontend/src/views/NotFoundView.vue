<template>
  <div class="not-found">
    <div class="code">404</div>
    <h2>页面不存在</h2>
    <p>你访问的资源已被移除，或当前角色没有对应菜单。</p>
    <el-button type="primary" @click="$router.push('/dashboard')"
      >返回工作台</el-button
    >
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 10px;
  background: #f4f6f9;
  text-align: center;
}

.code {
  font-size: 84px;
  font-weight: 800;
  letter-spacing: -0.04em;
  background: linear-gradient(135deg, #0f9d76, #244a6b);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

h2 {
  margin: 0;
  font-size: 20px;
}

p {
  margin: 0 0 12px;
  color: #6b7688;
  font-size: 13px;
}
</style>
