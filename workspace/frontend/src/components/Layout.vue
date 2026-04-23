<template>
  <div class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <h1>PSU全生命周期管理平台</h1>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleUserCommand">
          <span class="el-dropdown-link">
            {{ username }} <el-icon><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <slot></slot>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const username = ref('')

const handleUserCommand = (command) => {
  if (command === 'logout') {
    // 清除本地存储后返回默认首页
    localStorage.removeItem('token')
    router.push('/business')
  }
}

onMounted(() => {
  // 从本地存储获取用户名
  username.value = localStorage.getItem('username') || '用户'
})
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

.header-right {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
  color: white;
  display: flex;
  align-items: center;
}
</style>
