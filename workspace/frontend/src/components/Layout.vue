<template>
  <div class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <span class="header-badge">Prompt Ops</span>
        <h1>PSU 全生命周期管理平台</h1>
        <p class="header-subtitle">Design • Iterate • Review • Release</p>
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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  min-height: 72px;
  padding: 0 28px;
  color: var(--neo-text);
  border-bottom: 1px solid var(--neo-border);
  background: linear-gradient(120deg, rgba(15, 35, 58, 0.92), rgba(12, 23, 41, 0.86));
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
  position: sticky;
  top: 0;
  z-index: 40;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--neo-border-strong);
  background: rgba(77, 231, 200, 0.14);
  color: #b5fff3;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.header-left h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.header-subtitle {
  margin: 0;
  color: var(--neo-text-dim);
  font-size: 13px;
  letter-spacing: 0.06em;
}

@media (max-width: 768px) {
  .header {
    padding: 8px 14px;
    min-height: 64px;
  }

  .header-left h1 {
    font-size: 18px;
  }
}
</style>
