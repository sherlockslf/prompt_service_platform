<template>
  <div class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <h1>PSU全生命周期管理平台</h1>
      </div>
    </el-header>
    <slot></slot>
  </div>
</template>

<script setup>
// 当前版本头部仅展示平台标题，不展示登录/用户相关入口
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #409eff;
  color: white;
  padding: 0 20px;
}

.header-left h1 {
  margin: 0;
  font-size: 1.5em;
}

</style>
