<template>
  <div class="business-dashboard">
    <el-container>
      <el-aside width="200px" class="sidebar">
        <el-menu :default-active="activeMenu" class="menu" @select="handleMenuSelect">
          <el-menu-item index="1">
            <span>PSU列表</span>
          </el-menu-item>
          <el-menu-item index="2">
            <span>Prompt编排</span>
          </el-menu-item>
          <el-menu-item index="4">
            <span>版本提交</span>
          </el-menu-item>
          <el-menu-item index="5">
            <span>Schema编辑器</span>
          </el-menu-item>
          <el-menu-item index="6">
            <span>版本审核</span>
          </el-menu-item>
          <el-menu-item index="7">
            <span>代码生成</span>
          </el-menu-item>
          <el-menu-item index="8">
            <span>发版中心</span>
          </el-menu-item>
          <el-menu-item index="9">
            <span>系统管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <!-- PSU列表 -->
        <div v-if="activeMenu === '1'" class="content-section">
          <div class="header-section">
            <h2>PSU列表</h2>
            <el-button type="primary" @click="showCreatePsuDialog = true">新建PSU</el-button>
          </div>
          <el-table :data="psus" style="width: 100%; margin-top: 20px;" v-loading="loadingPsus">
            <el-table-column prop="psuId" label="PSU ID" width="200"></el-table-column>
            <el-table-column prop="name" label="名称" width="200"></el-table-column>
            <el-table-column prop="description" label="描述"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getPsuStatusTagType(row.status)">
                  {{ getPsuStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="360">
              <template #default="{ row }">
                <el-button size="small" @click="viewSchema(row)">查看Schema</el-button>
                <el-button size="small" @click="openEditPsuDialog(row)" :disabled="row.status !== 'DRAFT'">编辑</el-button>
                <el-button size="small" type="danger" @click="archivePsu(row)" :disabled="row.status === 'FORMAL'">归档</el-button>
                <el-button size="small" type="success" @click="submitVersionByPsu(row)" :disabled="row.status !== 'DRAFT'">提交审核(发布)</el-button>
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
                  {{ row.versionNo }}
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

        <!-- Schema编辑器 -->
        <div v-if="activeMenu === '5'" class="content-section">
          <h2>JSON Schema编辑器</h2>
          <el-form :model="schemaEditForm" label-width="120px">
            <el-form-item label="选择PSU">
              <el-select v-model="selectedSchemaPsuId" placeholder="请选择PSU" @change="loadSchemaForEdit">
                <el-option
                  v-for="psu in psus"
                  :key="psu.id"
                  :label="psu.name"
                  :value="psu.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="Schema内容">
              <el-input
                v-model="schemaEditForm.schemaContent"
                type="textarea"
                :rows="15"
                placeholder="请输入JSON Schema内容">
              </el-input>
            </el-form-item>
            <el-form-item label="变更日志">
              <el-input
                v-model="schemaEditForm.changeLog"
                type="textarea"
                :rows="3"
                placeholder="请输入变更日志">
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSchemaFromEditor">保存Schema</el-button>
              <el-button @click="loadSchemaForEdit">加载最新版本</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 版本审核 -->
        <div v-if="activeMenu === '6'" class="content-section">
          <h2>版本审核</h2>
          <el-table :data="versionStatus" style="width: 100%;" v-loading="loadingVersions">
            <el-table-column prop="psuId" label="PSU ID" width="150"></el-table-column>
            <el-table-column label="版本" width="150">
              <template #default="{ row }">
                {{ row.versionNo }}
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

        <!-- 代码生成 -->
        <div v-if="activeMenu === '7'" class="content-section">
          <h2>代码生成</h2>
          <el-form label-width="120px">
            <el-form-item label="选择PSU">
              <el-select v-model="selectedCodeGenPsuId" placeholder="请选择PSU">
                <el-option
                  v-for="psu in psus"
                  :key="psu.id"
                  :label="psu.name"
                  :value="psu.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="generateCodeInBusiness">生成代码</el-button>
              <el-button @click="downloadGeneratedCodeInBusiness">下载代码</el-button>
            </el-form-item>
          </el-form>
          <div v-if="generatedCodeInBusiness" class="code-preview">
            <h3>生成的代码预览</h3>
            <pre class="code-display">{{ generatedCodeInBusiness }}</pre>
          </div>
        </div>

        <!-- 发版中心 -->
        <div v-if="activeMenu === '8'" class="content-section">
          <ReleaseCenter />
        </div>

        <!-- 系统管理 -->
        <div v-if="activeMenu === '9'" class="content-section">
          <h2>系统管理</h2>
          <el-alert title="系统管理能力已在当前页统一导航下接入，后续可按需继续补充子模块。" type="info" :closable="false" show-icon />
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

    <!-- 创建PSU对话框 -->
    <el-dialog v-model="showCreatePsuDialog" title="新建PSU" width="600px">
      <el-form :model="newPsuForm" :rules="psuRules" ref="newPsuFormRef" label-width="100px">
        <el-form-item label="PSU ID" prop="psuId">
          <el-input v-model="newPsuForm.psuId" placeholder="请输入唯一的PSU ID"></el-input>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="newPsuForm.name" placeholder="请输入PSU名称"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="newPsuForm.description" type="textarea" :rows="3" placeholder="请输入PSU描述"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreatePsuDialog = false">取消</el-button>
        <el-button type="primary" @click="createPsu">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑PSU对话框 -->
    <el-dialog v-model="showEditPsuDialog" title="编辑PSU" width="600px">
      <el-form :model="editPsuForm" :rules="psuRules" ref="editPsuFormRef" label-width="100px">
        <el-form-item label="PSU ID">
          <el-input v-model="editPsuForm.psuId" disabled></el-input>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="editPsuForm.name" placeholder="请输入PSU名称"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="editPsuForm.description" type="textarea" :rows="3" placeholder="请输入PSU描述"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditPsuDialog = false">取消</el-button>
        <el-button type="primary" @click="updatePsu">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { psuApi, schemaApi, promptApi, versionApi, versionReviewApi } from '@/services/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import ReleaseCenter from '@/views/developer/ReleaseCenter.vue'

const activeMenu = ref('1')
const showSchemaDialog = ref(false)
const showCreatePsuDialog = ref(false)
const showEditPsuDialog = ref(false)
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
const selectedVersionPsuId = ref('')
const selectedSchemaPsuId = ref('')
const selectedCodeGenPsuId = ref('')
const newPsuFormRef = ref(null)
const editPsuFormRef = ref(null)
const newPsuForm = reactive({
  psuId: '',
  name: '',
  description: ''
})
const editPsuForm = reactive({
  id: null,
  psuId: '',
  name: '',
  description: ''
})
const psuRules = {
  psuId: [
    { required: true, message: '请输入PSU ID', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入PSU名称', trigger: 'blur' }
  ]
}

// Schema相关
const schemaFields = ref([])
const viewingSchemaFields = ref([])
const promptForm = reactive({
  promptContent: ''
})
const schemaEditForm = reactive({
  schemaContent: '',
  changeLog: ''
})

// 版本相关
const versionForm = reactive({
  description: ''
})
const versionStatus = ref([])
const generatedCodeInBusiness = ref('')

// 提交状态
const submitting = ref(false)

// Prompt编辑状态
const currentPromptEditable = ref(true)
const currentPromptFragmentId = ref(null)
const isBusinessUser = ref(true)

// 菜单选择处理
const handleMenuSelect = (index) => {
  // 统一在当前页面切换模块，避免左侧导航栏因跨路由跳转发生变化
  activeMenu.value = index
}

// 分页切换
const handlePageChange = (page) => {
  psuPagination.page = page
  loadPsus()
}

// 新建PSU：校验表单并调用后端创建接口
const createPsu = async () => {
  const valid = await newPsuFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  try {
    await psuApi.createPsu({
      psuId: newPsuForm.psuId,
      name: newPsuForm.name,
      description: newPsuForm.description
    })
    ElMessage.success('PSU创建成功')
    showCreatePsuDialog.value = false
    newPsuForm.psuId = ''
    newPsuForm.name = ''
    newPsuForm.description = ''
    await loadPsus()
  } catch (error) {
    console.error('创建PSU失败:', error)
    const backendMsg = error.response?.data?.message || error.response?.data?.error || ''
    if (error.response?.status === 409 || String(backendMsg).includes('PSU ID已存在') || String(backendMsg).includes('PSU ID already exists')) {
      ElMessage.error('PSU ID已存在，请更换后重试')
      return
    }
    ElMessage.error(backendMsg || '创建PSU失败')
  }
}

// 编辑PSU：将行数据拷贝到弹窗表单
const openEditPsuDialog = (psu) => {
  editPsuForm.id = psu.id
  editPsuForm.psuId = psu.psuId
  editPsuForm.name = psu.name
  editPsuForm.description = psu.description || ''
  showEditPsuDialog.value = true
}

// 更新PSU：校验表单后提交保存
const updatePsu = async () => {
  const valid = await editPsuFormRef.value?.validate().catch(() => false)
  if (!valid || !editPsuForm.id) {
    return
  }
  try {
    await psuApi.updatePsu(editPsuForm.id, {
      psuId: editPsuForm.psuId,
      name: editPsuForm.name,
      description: editPsuForm.description
    })
    ElMessage.success('PSU更新成功')
    showEditPsuDialog.value = false
    await loadPsus()
  } catch (error) {
    console.error('更新PSU失败:', error)
    ElMessage.error(error.response?.data?.message || '更新PSU失败')
  }
}

// 归档PSU：二次确认后执行软删除
const archivePsu = async (psu) => {
  try {
    await ElMessageBox.confirm(`确定要归档PSU "${psu.name}" 吗？`, '确认归档', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await psuApi.deletePsu(psu.id)
    ElMessage.success('PSU已归档')
    await loadPsus()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('归档PSU失败:', error)
      ElMessage.error('归档PSU失败')
    }
  }
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

const getPsuById = (id) => psus.value.find(item => item.id === id)

const isPsuDraft = (id) => getPsuById(id)?.status === 'DRAFT'

// 获取状态标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'CANDIDATE': return 'warning'
    case 'FORMAL': return 'success'
    case 'ARCHIVED': return 'info'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'CANDIDATE': return '发布候选'
    case 'FORMAL': return '正式版本'
    case 'ARCHIVED': return '归档'
    default: return status
  }
}

const getPsuStatusTagType = (status) => {
  switch (status) {
    case 'DRAFT': return ''
    case 'CANDIDATE': return 'warning'
    case 'FORMAL': return 'success'
    case 'ARCHIVED': return 'info'
    default: return 'info'
  }
}

const getPsuStatusText = (status) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'CANDIDATE': return '候选'
    case 'FORMAL': return '正式'
    case 'ARCHIVED': return '归档'
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
  if (!isPsuDraft(selectedPsuId.value)) {
    ElMessage.warning('仅草稿状态允许编辑Prompt')
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
  if (!isPsuDraft(selectedPsuId.value)) {
    ElMessage.warning('仅草稿状态允许定版')
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

// 在当前业务页内加载Schema编辑数据
const loadSchemaForEdit = async () => {
  if (!selectedSchemaPsuId.value) {
    schemaEditForm.schemaContent = ''
    schemaEditForm.changeLog = ''
    return
  }
  try {
    const response = await schemaApi.getSchema(selectedSchemaPsuId.value)
    schemaEditForm.schemaContent = response.data?.schemaContent || '{}'
    schemaEditForm.changeLog = response.data?.changeLog || ''
  } catch (error) {
    console.error('加载Schema失败:', error)
    schemaEditForm.schemaContent = '{}'
    schemaEditForm.changeLog = ''
    ElMessage.error('加载Schema失败')
  }
}

// 在当前业务页内保存Schema编辑数据
const saveSchemaFromEditor = async () => {
  if (!selectedSchemaPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  if (!isPsuDraft(selectedSchemaPsuId.value)) {
    ElMessage.warning('仅草稿状态允许编辑Schema')
    return
  }
  try {
    await schemaApi.updateSchema(selectedSchemaPsuId.value, {
      schemaContent: schemaEditForm.schemaContent,
      changeLog: schemaEditForm.changeLog
    })
    ElMessage.success('Schema保存成功')
  } catch (error) {
    console.error('保存Schema失败:', error)
    ElMessage.error('保存Schema失败')
  }
}

// 业务侧代码生成
const generateCodeInBusiness = async () => {
  if (!selectedCodeGenPsuId.value) {
    ElMessage.warning('请选择PSU')
    return
  }
  try {
    const response = await versionApi.getCode(selectedCodeGenPsuId.value)
    generatedCodeInBusiness.value = response.data
    ElMessage.success('代码生成成功')
  } catch (error) {
    console.error('生成代码失败:', error)
    ElMessage.error('生成代码失败')
  }
}

// 下载业务侧生成代码
const downloadGeneratedCodeInBusiness = () => {
  if (!generatedCodeInBusiness.value) {
    ElMessage.warning('请先生成代码')
    return
  }
  const blob = new Blob([generatedCodeInBusiness.value], { type: 'text/plain' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'generated-code.java'
  a.click()
  window.URL.revokeObjectURL(url)
}

// 提交版本
const submitVersion = async () => {
  if (!selectedVersionPsuId.value) {
    ElMessage.warning('请选择PSU')
    return
  }
  if (!isPsuDraft(selectedVersionPsuId.value)) {
    ElMessage.warning('仅草稿状态允许提交审核')
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

// 行内发布入口：直接按当前PSU提交版本审核
const submitVersionByPsu = async (psu) => {
  if (psu.status !== 'DRAFT') {
    ElMessage.warning('仅草稿状态允许提交审核')
    return
  }
  try {
    await versionApi.submitVersion(psu.id)
    ElMessage.success(`PSU ${psu.psuId} 已提交审核`)
    selectedVersionPsuId.value = psu.id
    activeMenu.value = '4'
    await loadVersionStatus()
  } catch (error) {
    console.error('提交版本失败:', error)
    ElMessage.error('提交失败')
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
    versionStatus.value = Array.isArray(response.data)
      ? response.data
      : (Array.isArray(response.data?.content) ? response.data.content : [])
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

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
