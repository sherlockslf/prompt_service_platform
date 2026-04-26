<template>
  <div class="admin-dashboard">
    <el-container>
      <el-aside :width="sidebarWidth" class="sidebar" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
        <el-menu default-active="1" class="menu" @select="handleMenuSelect">
          <el-menu-item index="1">
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="2">
            <span>系统配置</span>
          </el-menu-item>
          <el-menu-item index="3">
            <span>审计日志</span>
          </el-menu-item>
        </el-menu>
        <button
          class="sidebar-toggle"
          type="button"
          @click="toggleSidebar"
          :aria-label="isSidebarCollapsed ? '展开导航栏' : '收起导航栏'"
          :title="isSidebarCollapsed ? '展开导航栏' : '收起导航栏'"
        >
          <span :class="isSidebarCollapsed ? 'triangle-right' : 'triangle-left'"></span>
        </button>
      </el-aside>
      <el-main class="main-content" :class="{ 'main-content-collapsed': isSidebarCollapsed }">
        <div v-if="activeMenu === '1'" class="content-section">
          <h2>用户管理</h2>
          <el-button type="primary" @click="showCreateUserDialog = true">新增用户</el-button>
          <el-table :data="users" style="width: 100%; margin-top: 20px;">
            <el-table-column prop="username" label="用户名" width="180"></el-table-column>
            <el-table-column prop="role" label="角色" width="180"></el-table-column>
            <el-table-column prop="enabled" label="状态" width="180">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'danger'">
                  {{ row.enabled ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" @click="toggleUserStatus(row)">切换状态</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div v-if="activeMenu === '2'" class="content-section">
          <h2>系统配置</h2>
          <el-form :model="configForm" label-width="120px">
            <el-form-item label="API密钥">
              <el-input v-model="configForm.apiKey" type="password" placeholder="请输入API密钥"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <div v-if="activeMenu === '3'" class="content-section">
          <h2>审计日志</h2>
          <el-table :data="auditLogs" style="width: 100%;">
            <el-table-column prop="username" label="用户名" width="150"></el-table-column>
            <el-table-column prop="operation" label="操作" width="200"></el-table-column>
            <el-table-column prop="targetType" label="目标类型" width="150"></el-table-column>
            <el-table-column prop="createdAt" label="时间" width="200"></el-table-column>
            <el-table-column prop="details" label="详情"></el-table-column>
          </el-table>
        </div>
      </el-main>
    </el-container>
    
    <!-- 创建用户对话框 -->
    <el-dialog v-model="showCreateUserDialog" title="新增用户" width="500px">
      <el-form :model="newUserForm" :rules="userRules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="newUserForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="newUserForm.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="newUserForm.role" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN"></el-option>
            <el-option label="研发" value="DEVELOPER"></el-option>
            <el-option label="业务" value="BUSINESS"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch v-model="newUserForm.enabled" active-text="启用" inactive-text="禁用"></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateUserDialog = false">取消</el-button>
        <el-button type="primary" @click="createUser">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { userApi, auditLogApi, configApi } from '@/services/api'
import { ElMessage } from 'element-plus'

const activeMenu = ref('1')
const isSidebarCollapsed = ref(false)
const showCreateUserDialog = ref(false)

// 用户数据
const users = ref([])
const newUserForm = reactive({
  username: '',
  password: '',
  role: 'DEVELOPER',
  enabled: true
})

// 系统配置
const configForm = reactive({
  apiKey: ''
})

// 审计日志
const auditLogs = ref([])

// 表单验证规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 菜单选择处理
const handleMenuSelect = (index) => {
  activeMenu.value = index
}

const sidebarWidth = computed(() => (isSidebarCollapsed.value ? '56px' : '200px'))

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

// 切换用户状态
const toggleUserStatus = async (user) => {
  try {
    await userApi.toggleUserStatus(user.id)
    ElMessage.success('用户状态已更新')
    loadUsers()
  } catch (error) {
    console.error('切换用户状态失败:', error)
    ElMessage.error('切换用户状态失败')
  }
}

// 创建用户
const createUser = async () => {
  try {
    await userApi.createUser({
      username: newUserForm.username,
      password: newUserForm.password,
      role: newUserForm.role,
      enabled: newUserForm.enabled
    })
    ElMessage.success('用户创建成功')
    showCreateUserDialog.value = false
    newUserForm.username = ''
    newUserForm.password = ''
    newUserForm.role = 'DEVELOPER'
    newUserForm.enabled = true
    loadUsers()
  } catch (error) {
    console.error('创建用户失败:', error)
    ElMessage.error('创建用户失败')
  }
}

// 加载用户列表
const loadUsers = async () => {
  try {
    const response = await userApi.getUsers()
    users.value = response.data
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  }
}

// 加载审计日志
const loadAuditLogs = async () => {
  try {
    const response = await auditLogApi.getAllAuditLogs()
    auditLogs.value = response.data
  } catch (error) {
    console.error('加载审计日志失败:', error)
    auditLogs.value = []
  }
}

// 保存配置
const saveConfig = async () => {
  try {
    await configApi.saveConfig({
      configKey: 'default_api_key',
      configValue: configForm.apiKey,
      configType: 'API_KEY'
    })
    ElMessage.success('配置保存成功')
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存配置失败')
  }
}

onMounted(() => {
  loadUsers()
  loadAuditLogs()
})
</script>

<style scoped>
.admin-dashboard {
  --layout-header-height: 60px;
  --layout-sidebar-offset: 16px;
  min-height: calc(100vh - var(--layout-header-height));
}

.sidebar {
  background-color: #545c64;
  color: #fff;
  height: calc(100vh - var(--layout-header-height) - var(--layout-sidebar-offset));
  position: fixed;
  top: calc(var(--layout-header-height) + var(--layout-sidebar-offset));
  left: 0;
}

.sidebar-collapsed {
  overflow: visible;
}

.menu {
  border-right: none;
}

.sidebar-collapsed :deep(.el-menu-item span) {
  display: none;
}

.sidebar-collapsed :deep(.el-menu-item) {
  justify-content: center;
  padding: 0;
}

.sidebar-toggle {
  position: absolute;
  top: 12px;
  right: -14px;
  width: 28px;
  height: 28px;
  border: 1px solid #dcdfe6;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
}

.triangle-left {
  width: 0;
  height: 0;
  border-top: 7px solid transparent;
  border-bottom: 7px solid transparent;
  border-right: 10px solid #606266;
}

.triangle-right {
  width: 0;
  height: 0;
  border-top: 7px solid transparent;
  border-bottom: 7px solid transparent;
  border-left: 10px solid #606266;
}

.main-content {
  margin-left: 200px;
  min-height: calc(100vh - var(--layout-header-height));
  padding: 20px;
  overflow-y: auto;
}

.main-content-collapsed {
  margin-left: 56px;
}

.content-section {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
}
</style>
