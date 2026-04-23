<template>
  <div class="business-dashboard">
    <el-container>
      <el-aside width="200px" class="sidebar">
        <el-menu default-active="1" class="menu" @select="handleMenuSelect">
          <el-menu-item index="1">
            <span>PSU列表</span>
          </el-menu-item>
          <el-menu-item index="2">
            <span>Prompt编排</span>
          </el-menu-item>
          <el-menu-item index="3">
            <span>接口测试</span>
          </el-menu-item>
          <el-menu-item index="4">
            <span>版本提交</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <!-- PSU列表 -->
        <div v-if="activeMenu === '1'" class="content-section">
          <h2>PSU列表</h2>
          <el-table :data="psus" style="width: 100%; margin-top: 20px;" v-loading="loadingPsus">
            <el-table-column prop="psuId" label="PSU ID" width="200"></el-table-column>
            <el-table-column prop="name" label="名称" width="200"></el-table-column>
            <el-table-column prop="description" label="描述"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
                  {{ row.status === 'ACTIVE' ? '活跃' : '归档' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="viewSchema(row)">查看Schema</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="psuPagination.page"
            :page-size="psuPagination.size"
            :total="psuPagination.total"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
            style="margin-top: 20px; justify-content: flex-end;">
          </el-pagination>
        </div>
        
        <!-- Prompt编排 -->
        <div v-if="activeMenu === '2'" class="content-section">
          <h2>Prompt编排调试</h2>
          <el-form :model="promptForm" label-width="120px">
            <el-form-item label="选择PSU">
              <el-select v-model="selectedPsuId" placeholder="请选择PSU" @change="loadPromptFragments">
                <el-option 
                  v-for="psu in psus" 
                  :key="psu.id" 
                  :label="psu.name" 
                  :value="psu.id">
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item label="Schema字段台账" v-if="schemaFields.length > 0">
              <el-table :data="schemaFields" style="width: 100%; margin-top: 10px;">
                <el-table-column prop="name" label="字段名" width="200"></el-table-column>
                <el-table-column prop="type" label="类型" width="150"></el-table-column>
                <el-table-column prop="required" label="必填" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.required ? 'danger' : 'success'">
                      {{ row.required ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="描述"></el-table-column>
                <el-table-column label="操作" width="150">
                  <template #default="{ row }">
                    <el-button size="small" @click="insertVariable(row)">插入变量</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>
            
            <el-form-item label="Prompt内容">
              <div class="prompt-editor-container">
                <div class="prompt-editor-main">
                  <el-input 
                    v-model="promptForm.promptContent" 
                    type="textarea" 
                    :rows="10" 
                    placeholder="请输入Prompt内容，可使用上方变量"
                    :disabled="!currentPromptEditable">
                  </el-input>
                  <div class="prompt-status-bar">
                    <el-tag :type="currentPromptEditable ? 'success' : 'danger'" size="small">
                      {{ currentPromptEditable ? '可编辑' : '已定版' }}
                    </el-tag>
                    <el-button 
                      v-if="currentPromptEditable && isBusinessUser" 
                      size="small" 
                      type="warning" 
                      @click="finalizePrompt"
                      style="margin-left: 10px;">
                      定版
                    </el-button>
                  </div>
                </div>
                <div class="variable-panel" v-if="schemaFields.length > 0">
                  <h4>可用变量</h4>
                  <div class="variable-list">
                    <el-tag 
                      v-for="field in schemaFields" 
                      :key="field.name" 
                      class="variable-tag" 
                      @click="insertVariable(field)"
                      effect="dark">
                      {{ field.name }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="savePrompt">保存Prompt</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 接口测试 -->
        <div v-if="activeMenu === '3'" class="content-section">
          <h2>接口测试（编排容器下方）</h2>
          <el-form label-width="120px">
            <el-form-item label="选择PSU">
              <el-select v-model="selectedApiTestPsuId" placeholder="请选择PSU">
                <el-option 
                  v-for="psu in psus" 
                  :key="psu.id" 
                  :label="psu.name" 
                  :value="psu.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-alert
                title="运营侧接口测试已迁移到 Prompt 编排页下方，支持直接调用大模型进行多轮对话测试。"
                type="info"
                :closable="false"
                show-icon
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="goToApiTestInComposer">进入编排页测试</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 版本提交 -->
        <div v-if="activeMenu === '4'" class="content-section">
          <h2>版本提交与跟进</h2>
          <el-form :model="versionForm" label-width="120px">
            <el-form-item label="选择PSU">
              <el-select v-model="selectedVersionPsuId" placeholder="请选择PSU">
                <el-option 
                  v-for="psu in psus" 
                  :key="psu.id" 
                  :label="psu.name" 
                  :value="psu.id">
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item label="版本描述">
              <el-input 
                v-model="versionForm.description" 
                type="textarea" 
                :rows="4" 
                placeholder="请输入本次版本的改进内容和优化说明">
              </el-input>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="submitVersion" :loading="submitting">提交审核</el-button>
            </el-form-item>
          </el-form>
          
          <div class="version-status">
            <h3>审核状态</h3>
            <el-table :data="versionStatus" style="width: 100%;" v-loading="loadingVersions">
              <el-table-column prop="psuId" label="PSU ID" width="150"></el-table-column>
              <el-table-column label="版本" width="150">
                <template #default="{ row }">
                  {{ row.majorVersion }}.{{ row.minorVersion }}.{{ row.patchVersion }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="submittedAt" label="提交时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.submittedAt) }}
                </template>
              </el-table-column>
              <el-table-column prop="reviewedAt" label="审核时间" width="180">
                <template #default="{ row }">
                  {{ row.reviewedAt ? formatDateTime(row.reviewedAt) : '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="rejectionReason" label="备注"></el-table-column>
            </el-table>
          </div>
        </div>
      </el-main>
    </el-container>
    
    <!-- Schema查看对话框 -->
    <el-dialog v-model="showSchemaDialog" title="Schema详情" width="70%">
      <el-table :data="viewingSchemaFields" style="width: 100%;">
        <el-table-column prop="name" label="字段名" width="200"></el-table-column>
        <el-table-column prop="type" label="类型" width="150"></el-table-column>
        <el-table-column prop="required" label="必填" width="100">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'success'">
              {{ row.required ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述"></el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showSchemaDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { psuApi, schemaApi, promptApi, versionApi, versionReviewApi, testDatasetApi } from '@/services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeMenu = ref('1')
const showSchemaDialog = ref(false)
const loadingPsus = ref(false)
const loadingVersions = ref(false)

// PSU数据
const psus = ref([])
const psuPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const selectedPsuId = ref('')
const selectedApiTestPsuId = ref('')
const selectedVersionPsuId = ref('')

// Schema相关
const schemaFields = ref([])
const viewingSchemaFields = ref([])
const promptForm = reactive({
  promptContent: ''
})

// 版本相关
const versionForm = reactive({
  description: ''
})
const versionStatus = ref([])

// 提交状态
const submitting = ref(false)

// Prompt编辑状态
const currentPromptEditable = ref(true)
const currentPromptFragmentId = ref(null)
const isBusinessUser = ref(true)

// 菜单选择处理
const handleMenuSelect = (index) => {
  if (index === '2') {
    // Prompt编排菜单项，导航到PromptComposer
    router.push('/business/composer')
  } else {
    activeMenu.value = index
  }
}

// 分页切换
const handlePageChange = (page) => {
  psuPagination.page = page
  loadPsus()
}

// 查看Schema
const viewSchema = async (psu) => {
  try {
    const response = await schemaApi.getSchema(psu.id)
    let schemaContent = response.data.schemaContent || '{}'
    if (typeof schemaContent === 'string') {
      schemaContent = JSON.parse(schemaContent)
    }
    viewingSchemaFields.value = parseSchemaFields(schemaContent)
    showSchemaDialog.value = true
  } catch (error) {
    console.error('获取Schema失败:', error)
    ElMessage.error('获取Schema失败')
  }
}

// 解析Schema字段(支持标准JSON Schema格式和扁平JSON格式)
const parseSchemaFields = (schema) => {
  const fields = []
  
  // 标准JSON Schema格式: { properties: { ... }, required: [...] }
  if (schema.properties) {
    for (const [name, prop] of Object.entries(schema.properties)) {
      fields.push({
        name,
        type: prop.type || 'string',
        required: (schema.required || []).includes(name),
        description: prop.description || ''
      })
    }
  } 
  // 扁平JSON格式: { key1: value1, key2: value2, ... }
  else {
    for (const [name, value] of Object.entries(schema)) {
      fields.push({
        name,
        type: inferType(value),
        required: false,
        description: ''
      })
    }
  }
  
  return fields
}

// 推断JSON值的类型
const inferType = (value) => {
  if (value === null || value === undefined) return 'null'
  if (Array.isArray(value)) return 'array'
  const type = typeof value
  if (type === 'number') return 'number'
  if (type === 'boolean') return 'boolean'
  if (type === 'object') return 'object'
  return 'string'
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case 'PENDING': return 'warning'
    case 'APPROVED': return 'success'
    case 'REJECTED': return 'danger'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '待审核'
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已拒绝'
    default: return status
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit', 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit',
    hour12: false 
  })
}

// 加载Prompt片段
const loadPromptFragments = async () => {
  if (!selectedPsuId.value) return
  
  try {
    // 先加载Schema获取字段
    const schemaResponse = await schemaApi.getSchema(selectedPsuId.value)
    let schemaContent = schemaResponse.data.schemaContent || '{}'
    if (typeof schemaContent === 'string') {
      schemaContent = JSON.parse(schemaContent)
    }
    schemaFields.value = parseSchemaFields(schemaContent)
    
    // 加载Prompt片段
    const promptResponse = await promptApi.getPromptFragments(selectedPsuId.value)
    let coreRules = null
    
    if (promptResponse.data && promptResponse.data.length > 0) {
      coreRules = promptResponse.data.find(p => p.fragmentKey === 'core_rules')
    }
    
    if (coreRules) {
      // 如果找到了核心规则片段
      promptForm.promptContent = coreRules.content
      currentPromptFragmentId.value = coreRules.id
      currentPromptEditable.value = coreRules.editable !== false
    } else {
      // 如果没有找到核心规则片段，初始化为空内容
      promptForm.promptContent = ''
      currentPromptFragmentId.value = null
      currentPromptEditable.value = true
      
      // 检查是否有可编辑的片段，如果有则使用第一个
      if (promptResponse.data && promptResponse.data.length > 0) {
        const firstEditable = promptResponse.data.find(p => p.editable)
        if (firstEditable) {
          promptForm.promptContent = firstEditable.content
          currentPromptFragmentId.value = firstEditable.id
          currentPromptEditable.value = firstEditable.editable !== false
        }
      }
    }
    
  } catch (error) {
    console.error('加载Prompt片段失败:', error)
    ElMessage.error('加载Prompt片段失败')
  }
}

// 插入变量
const insertVariable = (field) => {
  const cursorPosition = getCursorPosition()
  const textBefore = promptForm.promptContent.substring(0, cursorPosition)
  const textAfter = promptForm.promptContent.substring(cursorPosition)
  promptForm.promptContent = `${textBefore}{{${field.name}}}${textAfter}`
}

// 获取光标位置（简化实现）
const getCursorPosition = () => {
  return promptForm.promptContent.length
}

// 保存Prompt
const savePrompt = async () => {
  if (!selectedPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  
  if (!currentPromptEditable.value) {
    ElMessage.warning('Prompt已定版，不允许修改')
    return
  }
  
  try {
    if (currentPromptFragmentId.value) {
      // 更新现有的Prompt片段
      await promptApi.updatePromptFragment(currentPromptFragmentId.value, {
        content: promptForm.promptContent
      })
    } else {
      // 创建新的Prompt片段
      await promptApi.createPromptFragment({
        psuId: selectedPsuId.value,
        fragmentKey: 'core_rules',
        content: promptForm.promptContent,
        editable: true,
        type: 'CORE_RULES',
        sortOrder: 0
      })
      
      // 重新加载片段以获取新创建的ID
      const promptResponse = await promptApi.getPromptFragments(selectedPsuId.value)
      const newCoreRules = promptResponse.data.find(p => p.fragmentKey === 'core_rules')
      if (newCoreRules) {
        currentPromptFragmentId.value = newCoreRules.id
      }
    }
    ElMessage.success('Prompt保存成功')
  } catch (error) {
    console.error('保存Prompt失败:', error)
    ElMessage.error('保存Prompt失败')
  }
}

// 定版Prompt
const finalizePrompt = async () => {
  if (!currentPromptFragmentId.value) {
    ElMessage.warning('请先加载Prompt片段')
    return
  }
  
  try {
    await promptApi.finalizePromptFragment(currentPromptFragmentId.value)
    ElMessage.success('Prompt已定版，所有人不可修改')
    currentPromptEditable.value = false
  } catch (error) {
    console.error('定版Prompt失败:', error)
    ElMessage.error('定版失败')
  }
}

const goToApiTestInComposer = () => {
  if (!selectedApiTestPsuId.value) {
    router.push('/business/composer')
  } else {
    router.push(`/business/psus/${selectedApiTestPsuId.value}/composer`)
  }
}

// 提交版本
const submitVersion = async () => {
  if (!selectedVersionPsuId.value) {
    ElMessage.warning('请选择PSU')
    return
  }
  
  submitting.value = true
  try {
    await versionApi.submitVersion(selectedVersionPsuId.value)
    ElMessage.success('版本提交成功，等待研发审核')
    submitting.value = false
    versionForm.description = ''
    loadVersionStatus()
  } catch (error) {
    console.error('提交版本失败:', error)
    ElMessage.error('提交失败')
    submitting.value = false
  }
}

// 加载PSU列表
const loadPsus = async () => {
  loadingPsus.value = true
  try {
    const response = await psuApi.getPsus(psuPagination.page, psuPagination.size)
    psus.value = response.data.content
    psuPagination.total = response.data.totalElements
  } catch (error) {
    console.error('加载PSU列表失败:', error)
    ElMessage.error('加载PSU列表失败')
  } finally {
    loadingPsus.value = false
  }
}

// 加载版本状态
const loadVersionStatus = async () => {
  loadingVersions.value = true
  try {
    const response = await versionReviewApi.getVersionReviews()
    versionStatus.value = response.data
  } catch (error) {
    console.error('加载版本状态失败:', error)
    versionStatus.value = []
  } finally {
    loadingVersions.value = false
  }
}

onMounted(() => {
  loadPsus()
  loadVersionStatus()
})
</script>

<style scoped>
.business-dashboard {
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

.menu {
  border-right: none;
}

.main-content {
  margin-left: 200px;
  min-height: calc(100vh - var(--layout-header-height));
  padding: 20px;
  overflow-y: auto;
}

.content-section {
  padding: 20px;
}

.prompt-editor-container {
  display: flex;
  gap: 20px;
}

.variable-panel {
  width: 200px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  background-color: #f5f7fa;
}

.variable-list {
  margin-top: 10px;
}

.variable-tag {
  margin: 5px;
  cursor: pointer;
}

.input-field {
  margin-bottom: 15px;
}

.input-field label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.version-status {
  margin-top: 30px;
}

h2, h3 {
  margin-bottom: 20px;
}
</style>
