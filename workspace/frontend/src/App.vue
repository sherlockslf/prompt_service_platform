<template>
  <div id="app">
    <Layout v-if="$route.path !== '/login'">
      <router-view />
    </Layout>
    <router-view v-else />
  </div>
</template>

<script setup>
// PSU全生命周期管理平台根组件
import Layout from '@/components/Layout.vue'
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&display=swap');

:root {
  --neo-bg-0: #070b14;
  --neo-bg-1: #0a1120;
  --neo-bg-2: #0e1a2e;
  --neo-surface: rgba(15, 26, 46, 0.74);
  --neo-surface-soft: rgba(17, 33, 55, 0.58);
  --neo-border: rgba(90, 203, 255, 0.22);
  --neo-border-strong: rgba(96, 248, 214, 0.46);
  --neo-text: #e8f7ff;
  --neo-text-dim: #9cb7ca;
  --neo-accent: #4de7c8;
  --neo-accent-2: #57a8ff;
  --neo-danger: #ff6d8a;
  --neo-radius-lg: 16px;
  --neo-radius-md: 12px;
  --neo-shadow: 0 20px 45px rgba(0, 0, 0, 0.4);
}

html,
body {
  margin: 0;
  padding: 0;
  min-height: 100%;
  background:
    radial-gradient(1000px 500px at 6% -10%, rgba(79, 171, 255, 0.34), transparent 65%),
    radial-gradient(900px 480px at 98% 8%, rgba(77, 231, 200, 0.24), transparent 68%),
    linear-gradient(155deg, var(--neo-bg-0), var(--neo-bg-1) 45%, var(--neo-bg-2));
  color: var(--neo-text);
}

#app {
  font-family: 'Space Grotesk', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
  color: var(--neo-text);
}

#app::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 22px 22px;
  opacity: 0.3;
  z-index: 0;
}

#app > * {
  position: relative;
  z-index: 1;
}

.el-message {
  border-radius: 999px !important;
  border: 1px solid var(--neo-border) !important;
  background: rgba(7, 20, 36, 0.9) !important;
  color: var(--neo-text) !important;
  backdrop-filter: blur(10px);
}

.el-overlay-dialog .el-dialog {
  background: rgba(10, 22, 40, 0.94) !important;
  border: 1px solid var(--neo-border) !important;
  border-radius: var(--neo-radius-lg) !important;
  box-shadow: var(--neo-shadow) !important;
}

.el-overlay-dialog .el-dialog__title {
  color: var(--neo-text) !important;
}

.el-overlay-dialog .el-dialog__body {
  color: var(--neo-text-dim) !important;
}

/* Global dark tech-skin for Element Plus */
.el-card {
  background: var(--neo-surface) !important;
  border: 1px solid var(--neo-border) !important;
  color: var(--neo-text) !important;
}

.el-card__header {
  border-bottom: 1px solid rgba(95, 158, 199, 0.22) !important;
  color: var(--neo-text) !important;
}

.el-table,
.el-table__inner-wrapper,
.el-table th.el-table__cell,
.el-table td.el-table__cell,
.el-table tr {
  background: transparent !important;
  color: var(--neo-text) !important;
}

.el-table::before,
.el-table__inner-wrapper::before {
  background-color: rgba(95, 158, 199, 0.2) !important;
}

.el-table th.el-table__cell {
  color: #c4e4f7 !important;
  border-bottom: 1px solid rgba(95, 158, 199, 0.24) !important;
}

.el-table td.el-table__cell {
  border-bottom: 1px solid rgba(95, 158, 199, 0.16) !important;
}

.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell {
  background: rgba(77, 231, 200, 0.04) !important;
}

.el-form-item__label,
.el-descriptions__label,
.el-descriptions__content {
  color: var(--neo-text) !important;
}

.el-input__wrapper,
.el-textarea__inner,
.el-select__wrapper,
.el-input-group__append,
.el-input-group__prepend {
  background: rgba(6, 16, 30, 0.88) !important;
  box-shadow: inset 0 0 0 1px var(--neo-border) !important;
  color: var(--neo-text) !important;
}

.el-input__inner,
.el-select__placeholder,
.el-textarea__inner::placeholder,
.el-input__inner::placeholder {
  color: var(--neo-text-dim) !important;
}

.el-select-dropdown,
.el-popper.is-light,
.el-picker-panel,
.el-dropdown-menu {
  background: rgba(8, 19, 35, 0.96) !important;
  border: 1px solid var(--neo-border) !important;
  color: var(--neo-text) !important;
}

.el-select-dropdown__item,
.el-dropdown-menu__item {
  color: var(--neo-text) !important;
}

.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover,
.el-dropdown-menu__item:hover {
  background: rgba(77, 231, 200, 0.14) !important;
  color: #e7fffa !important;
}

.el-pagination .btn-next,
.el-pagination .btn-prev,
.el-pager li {
  background: rgba(8, 19, 35, 0.88) !important;
  color: var(--neo-text) !important;
}

.el-pagination .btn-next:disabled,
.el-pagination .btn-prev:disabled {
  color: #6f8aa0 !important;
}

.el-button {
  border-color: var(--neo-border) !important;
  background: rgba(10, 24, 43, 0.84) !important;
  color: var(--neo-text) !important;
}

.el-button:hover {
  color: #e7fffa !important;
  border-color: var(--neo-border-strong) !important;
  background: rgba(77, 231, 200, 0.18) !important;
}

.el-button--primary {
  border: none !important;
  background: linear-gradient(110deg, var(--neo-accent), var(--neo-accent-2)) !important;
  color: #071120 !important;
}

.el-button--success {
  border: none !important;
  background: linear-gradient(110deg, #7bf6ca, #43d7a8) !important;
  color: #081b13 !important;
}

.el-button--warning {
  border: none !important;
  background: linear-gradient(110deg, #ffd35b, #ff9f4d) !important;
  color: #1d1002 !important;
}

.el-button--danger {
  border: none !important;
  background: linear-gradient(110deg, #8a3049, #6f253a) !important;
  color: #ffeef2 !important;
}

.el-tag {
  border-color: rgba(95, 158, 199, 0.28) !important;
  background: rgba(12, 30, 52, 0.72) !important;
  color: var(--neo-text) !important;
}

.el-alert {
  background: rgba(13, 29, 50, 0.86) !important;
  border: 1px solid rgba(95, 158, 199, 0.24) !important;
}

.el-alert__title,
.el-alert__description {
  color: var(--neo-text) !important;
}
</style>
