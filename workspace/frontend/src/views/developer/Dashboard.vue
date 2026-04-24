<template>
  <div class="developer-dashboard">
    <el-container>
      <el-aside width="200px" class="sidebar">
        <el-menu default-active="1" class="menu" @select="handleMenuSelect">
          <el-menu-item index="1">
            <span>PSU管理</span>
          </el-menu-item>
          <el-menu-item index="2">
            <span>Schema编辑器</span>
          </el-menu-item>
          <el-menu-item index="3">
            <span>Prompt管理</span>
          </el-menu-item>
          <el-menu-item index="4">
            <span>版本审核</span>
          </el-menu-item>
          <el-menu-item index="5">
            <span>代码生成</span>
          </el-menu-item>
          <el-menu-item index="6">
            <span>发版中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <!-- PSU管理 -->
        <div v-if="activeMenu === '1'" class="content-section">
          <div class="header-section">
            <h2>PSU管理</h2>
            <el-button type="primary" @click="showCreatePsuDialog = true">新建PSU</el-button>
          </div>
          <el-table :data="psus" style="width: 100%; margin-top: 20px;">
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
            <el-table-column prop="updatedAt" label="更新时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="editPsu(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="archivePsu(row)">归档</el-button>
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
        
        <!-- Schema编辑器 -->
        <div v-if="activeMenu === '2'" class="content-section">
          <h2>JSON Schema编辑器</h2>
          <el-form :model="schemaForm" label-width="120px">
            <el-form-item label="选择PSU">
              <el-select v-model="selectedPsuId" placeholder="请选择PSU" @change="loadSchema">
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
                v-model="schemaForm.schemaContent" 
                type="textarea" 
                :rows="15" 
                placeholder="请输入JSON Schema内容">
              </el-input>
            </el-form-item>
            <el-form-item label="变更日志">
              <el-input 
                v-model="schemaForm.changeLog" 
                type="textarea" 
                :rows="3" 
                placeholder="请输入变更日志">
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSchema">保存Schema</el-button>
              <el-button @click="loadSchema">加载最新版本</el-button>
            </el-form-item>
          </el-form>
          
          <!-- 测试数据集管理 -->
          <div class="test-dataset-section" v-if="selectedPsuId">
            <div class="section-header">
              <h3>测试数据集</h3>
              <el-button type="primary" size="small" @click="showCreateDatasetDialog = true">新建数据集</el-button>
            </div>
            <el-table :data="testDatasets" style="width: 100%; margin-top: 10px;" v-loading="loadingDatasets">
              <el-table-column prop="name" label="数据集名称" width="200"></el-table-column>
              <el-table-column prop="description" label="描述" width="300"></el-table-column>
              <el-table-column label="数据内容" min-width="200">
                <template #default="{ row }">
                  <el-button size="small" @click="viewDatasetData(row)">查看</el-button>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <el-button size="small" @click="editDataset(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteDataset(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
        
        <!-- Prompt管理 -->
        <div v-if="activeMenu === '3'" class="content-section">
          <h2>Prompt片段管理</h2>
          <el-form :model="promptForm" label-width="120px">
            <el-form-item label="选择PSU">
              <el-select v-model="selectedPromptPsuId" placeholder="请选择PSU" @change="loadPromptFragments">
                <el-option 
                  v-for="psu in psus" 
                  :key="psu.id" 
                  :label="psu.name" 
                  :value="psu.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-table :data="promptFragments" style="width: 100%; margin-top: 20px;">
              <el-table-column prop="fragmentKey" label="片段标识" width="200"></el-table-column>
              <el-table-column prop="type" label="类型" width="150">
                <template #default="{ row }">
                  <el-tag :type="row.type === 'CORE_RULES' ? 'danger' : 'primary'">
                    {{ row.type === 'CORE_RULES' ? '核心规则' : '消息模板' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="editable" label="可编辑" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.editable ? 'success' : 'info'">
                    {{ row.editable ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="content" label="内容"></el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button size="small" @click="editPromptFragment(row)" :disabled="!row.editable">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-form>
        </div>
        
        <!-- 版本审核 -->
        <div v-if="activeMenu === '4'" class="content-section">
          <h2>版本审核</h2>
          <el-table :data="versionReviews" style="width: 100%;">
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
            <el-table-column prop="submitterId" label="提交者" width="120"></el-table-column>
            <el-table-column prop="submittedAt" label="提交时间" width="180"></el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="approveVersion(row)" :disabled="row.status !== 'PENDING'">通过</el-button>
                <el-button size="small" type="danger" @click="rejectVersion(row)" :disabled="row.status !== 'PENDING'">拒绝</el-button>
                <el-button size="small" @click="viewCode(row)" v-if="row.codeContent">查看代码</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="versionReviewPagination.page"
            :page-size="versionReviewPagination.size"
            :total="versionReviewPagination.total"
            layout="total, prev, pager, next"
            @current-change="handleVersionReviewPageChange"
            style="margin-top: 20px; justify-content: flex-end;">
          </el-pagination>
        </div>
        
        <!-- 代码生成 -->
        <div v-if="activeMenu === '5'" class="content-section">
          <h2>代码生成</h2>
          <el-form :model="codeGenForm" label-width="120px">
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
              <el-button type="primary" @click="generateCode">生成代码</el-button>
              <el-button @click="downloadCode">下载代码</el-button>
            </el-form-item>
          </el-form>
          <div v-if="generatedCode" class="code-preview">
            <h3>生成的代码预览</h3>
            <pre>{{ generatedCode }}</pre>
          </div>
        </div>
      </el-main>
    </el-container>
    
    <!-- 创建PSU对话框 -->
    <el-dialog v-model="showCreatePsuDialog" title="新建PSU" width="600px">
      <el-form :model="newPsuForm" :rules="psuRules" ref="psuFormRef" label-width="100px">
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
    
    <!-- 编辑Prompt片段对话框 -->
    <el-dialog v-model="showEditPromptDialog" title="编辑Prompt片段" width="700px">
      <el-form :model="editingPromptFragment" label-width="100px">
        <el-form-item label="片段标识">
          <el-input v-model="editingPromptFragment.fragmentKey" disabled></el-input>
        </el-form-item>
        <el-form-item label="内容">
          <el-input 
            v-model="editingPromptFragment.content" 
            type="textarea" 
            :rows="8" 
            placeholder="请输入Prompt内容">
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditPromptDialog = false">取消</el-button>
        <el-button type="primary" @click="savePromptFragment">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- 查看代码对话框 -->
    <el-dialog v-model="showCodeDialog" title="生成的代码" width="80%" top="5vh">
      <pre class="code-display">{{ viewingCode }}</pre>
      <template #footer>
        <el-button @click="showCodeDialog = false">关闭</el-button>
        <el-button type="primary" @click="copyCode">复制代码</el-button>
      </template>
    </el-dialog>
    
    <!-- 创建/编辑测试数据集对话框 -->
    <el-dialog v-model="showCreateDatasetDialog" :title="editingDatasetId ? '编辑测试数据集' : '新建测试数据集'" width="700px">
      <el-form :model="datasetForm" label-width="100px">
        <el-form-item label="数据集名称">
          <el-input v-model="datasetForm.name" placeholder="请输入数据集名称"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="datasetForm.description" type="textarea" :rows="2" placeholder="请输入描述"></el-input>
        </el-form-item>
        <el-form-item label="数据内容">
          <el-input 
            v-model="datasetForm.dataContent" 
            type="textarea" 
            :rows="10" 
            placeholder="请输入JSON格式的测试数据">
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDatasetDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDataset">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- 查看数据集数据对话框 -->
    <el-dialog v-model="showViewDatasetDialog" title="数据内容" width="700px">
      <pre class="data-display">{{ viewingDatasetData }}</pre>
      <template #footer>
        <el-button @click="showViewDatasetDialog = false">关闭</el-button>
        <el-button type="primary" @click="copyDatasetData">复制数据</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api, { psuApi, schemaApi, testDatasetApi, versionReviewApi } from '@/services/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const activeMenu = ref('1')
const showCreatePsuDialog = ref(false)
const showEditPsuDialog = ref(false)
const showEditPromptDialog = ref(false)
const showCodeDialog = ref(false)
const showCreateDatasetDialog = ref(false)
const showViewDatasetDialog = ref(false)

// PSU数据
const psus = ref([])
const psuPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
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

// Schema数据
const selectedPsuId = ref('')
const schemaForm = reactive({
  schemaContent: '',
  changeLog: ''
})

// Prompt数据
const selectedPromptPsuId = ref('')
const promptFragments = ref([])
const editingPromptFragment = ref({})

// 版本审核数据
const versionReviews = ref([])
const versionReviewPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 代码生成数据
const selectedCodeGenPsuId = ref('')
const generatedCode = ref('')
const viewingCode = ref('')

// 测试数据集数据
const testDatasets = ref([])
const loadingDatasets = ref(false)
const editingDatasetId = ref(null)
const datasetForm = reactive({
  name: '',
  description: '',
  dataContent: ''
})
const viewingDatasetData = ref('')

// 表单验证规则
const psuRules = {
  psuId: [
    { required: true, message: '请输入PSU ID', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入PSU名称', trigger: 'blur' }
  ]
}

// 菜单选择处理
const handleMenuSelect = (index) => {
  if (index === '3') {
    // 统一将开发侧Prompt管理入口切换到动态容器编排页面
    if (selectedPsuId.value) {
      router.push(`/business/psus/${selectedPsuId.value}/composer`)
      return
    }
    router.push('/business/composer')
    return
  }
  if (index === '6') {
    router.push('/developer/releases')
    return
  }
  activeMenu.value = index
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

// 创建PSU
const createPsu = async () => {
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
    loadPsus()
  } catch (error) {
    console.error('创建PSU失败:', error)
    const backendMsg = error.response?.data?.message || error.response?.data?.error || ''
    if (error.response?.status === 409 || String(backendMsg).includes('PSU ID already exists')) {
      ElMessage.error('PSU ID已存在，请更换后重试')
    } else if (error.response?.status === 403) {
      ElMessage.error('无权限创建PSU，请确认当前账号角色')
    } else {
      ElMessage.error(backendMsg || '创建PSU失败')
    }
  }
}

// 编辑PSU
const editPsu = (psu) => {
  editPsuForm.id = psu.id
  editPsuForm.psuId = psu.psuId
  editPsuForm.name = psu.name
  editPsuForm.description = psu.description
  showEditPsuDialog.value = true
}

// 更新PSU
const updatePsu = async () => {
  try {
    await psuApi.updatePsu(editPsuForm.id, {
      psuId: editPsuForm.psuId,
      name: editPsuForm.name,
      description: editPsuForm.description
    })
    ElMessage.success('PSU更新成功')
    showEditPsuDialog.value = false
    loadPsus()
  } catch (error) {
    console.error('更新PSU失败:', error)
    ElMessage.error('更新PSU失败')
  }
}

// 归档PSU
const archivePsu = async (psu) => {
  try {
    await ElMessageBox.confirm(`确定要归档PSU "${psu.name}" 吗？`, '确认归档', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await psuApi.deletePsu(psu.id)
    ElMessage.success('PSU已归档')
    loadPsus()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('归档PSU失败:', error)
      ElMessage.error('归档PSU失败')
    }
  }
}

// 分页切换
const handlePageChange = (page) => {
  psuPagination.page = page
  loadPsus()
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
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

// 保存Schema
const saveSchema = async () => {
  try {
    await api.put(`/schemas/${selectedPsuId.value}`, {
      schemaContent: schemaForm.schemaContent,
      changeLog: schemaForm.changeLog
    })
    ElMessage.success('Schema保存成功')
  } catch (error) {
    console.error('保存Schema失败:', error)
    ElMessage.error('保存Schema失败')
  }
}

// 加载Schema
const loadSchema = async () => {
  if (!selectedPsuId.value) return
  
  try {
    const response = await schemaApi.getSchema(selectedPsuId.value)
    schemaForm.schemaContent = response.data.schemaContent
    schemaForm.changeLog = response.data.changeLog || ''
    // 加载Schema后同时加载测试数据集
    loadTestDatasets()
  } catch (error) {
    console.error('加载Schema失败:', error)
    schemaForm.schemaContent = '{}'
    schemaForm.changeLog = ''
  }
}

// 加载测试数据集
const loadTestDatasets = async () => {
  if (!selectedPsuId.value) return
  
  loadingDatasets.value = true
  try {
    const response = await testDatasetApi.getTestDatasets(selectedPsuId.value)
    testDatasets.value = response.data
  } catch (error) {
    console.error('加载测试数据集失败:', error)
    testDatasets.value = []
  } finally {
    loadingDatasets.value = false
  }
}

// 保存测试数据集
const saveDataset = async () => {
  if (!datasetForm.name || !datasetForm.dataContent) {
    ElMessage.warning('请填写数据集名称和数据内容')
    return
  }
  
  try {
    if (editingDatasetId.value) {
      await testDatasetApi.updateTestDataset(editingDatasetId.value, {
        name: datasetForm.name,
        description: datasetForm.description,
        dataContent: datasetForm.dataContent
      })
      ElMessage.success('数据集更新成功')
    } else {
      await testDatasetApi.createTestDataset(selectedPsuId.value, {
        name: datasetForm.name,
        description: datasetForm.description,
        dataContent: datasetForm.dataContent
      })
      ElMessage.success('数据集创建成功')
    }
    showCreateDatasetDialog.value = false
    resetDatasetForm()
    loadTestDatasets()
  } catch (error) {
    console.error('保存数据集失败:', error)
    ElMessage.error('保存数据集失败')
  }
}

// 编辑数据集
const editDataset = (dataset) => {
  editingDatasetId.value = dataset.id
  datasetForm.name = dataset.name
  datasetForm.description = dataset.description || ''
  datasetForm.dataContent = dataset.dataContent
  showCreateDatasetDialog.value = true
}

// 删除数据集
const deleteDataset = async (dataset) => {
  try {
    await ElMessageBox.confirm(`确定要删除数据集 "${dataset.name}" 吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await testDatasetApi.deleteTestDataset(dataset.id)
    ElMessage.success('数据集已删除')
    loadTestDatasets()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除数据集失败:', error)
      ElMessage.error('删除数据集失败')
    }
  }
}

// 查看数据集数据
const viewDatasetData = (dataset) => {
  viewingDatasetData.value = dataset.dataContent
  showViewDatasetDialog.value = true
}

// 复制数据集数据
const copyDatasetData = () => {
  navigator.clipboard.writeText(viewingDatasetData.value)
  ElMessage.success('数据已复制到剪贴板')
}

// 重置数据集表单
const resetDatasetForm = () => {
  editingDatasetId.value = null
  datasetForm.name = ''
  datasetForm.description = ''
  datasetForm.dataContent = ''
}

// 加载Prompt片段
const loadPromptFragments = async () => {
  if (!selectedPromptPsuId.value) return
  
  try {
    const response = await api.get(`/prompts/${selectedPromptPsuId.value}`)
    promptFragments.value = response.data
  } catch (error) {
    console.error('加载Prompt片段失败:', error)
    promptFragments.value = []
  }
}

// 编辑Prompt片段
const editPromptFragment = (fragment) => {
  editingPromptFragment.value = { ...fragment }
  showEditPromptDialog.value = true
}

// 保存Prompt片段
const savePromptFragment = async () => {
  try {
    await api.put(`/prompts/${editingPromptFragment.value.id}`, {
      content: editingPromptFragment.value.content
    })
    ElMessage.success('Prompt片段保存成功')
    showEditPromptDialog.value = false
    loadPromptFragments()
  } catch (error) {
    console.error('保存Prompt片段失败:', error)
    ElMessage.error('保存Prompt片段失败')
  }
}

// 通过版本
const approveVersion = async (review) => {
  try {
    await versionReviewApi.reviewVersion(review.id, {
      approved: true
    })
    ElMessage.success('版本审核通过')
    loadVersionReviews()
  } catch (error) {
    console.error('审核版本失败:', error)
    ElMessage.error('审核版本失败')
  }
}

// 拒绝版本
const rejectVersion = async (review) => {
  try {
    await versionReviewApi.reviewVersion(review.id, {
      approved: false,
      rejectionReason: '不符合要求'
    })
    ElMessage.success('版本审核已拒绝')
    loadVersionReviews()
  } catch (error) {
    console.error('审核版本失败:', error)
    ElMessage.error('审核版本失败')
  }
}

// 查看代码
const viewCode = async (review) => {
  try {
    const response = await versionReviewApi.getCode(review.psuId)
    viewingCode.value = response.data
    showCodeDialog.value = true
  } catch (error) {
    console.error('获取代码失败:', error)
    ElMessage.error('获取代码失败')
  }
}

// 生成代码
const generateCode = async () => {
  if (!selectedCodeGenPsuId.value) {
    ElMessage.warning('请选择PSU')
    return
  }
  
  try {
    const response = await versionReviewApi.getCode(selectedCodeGenPsuId.value)
    generatedCode.value = response.data
    ElMessage.success('代码生成成功')
  } catch (error) {
    console.error('生成代码失败:', error)
    ElMessage.error('生成代码失败')
  }
}

// 下载代码
const downloadCode = () => {
  if (!generatedCode.value) {
    ElMessage.warning('请先生成代码')
    return
  }
  
  const blob = new Blob([generatedCode.value], { type: 'text/plain' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'generated-code.java'
  a.click()
  window.URL.revokeObjectURL(url)
}

// 复制代码
const copyCode = () => {
  navigator.clipboard.writeText(viewingCode.value)
  ElMessage.success('代码已复制到剪贴板')
}

// 加载PSU列表
const loadPsus = async () => {
  try {
    const response = await psuApi.getPsus(psuPagination.page, psuPagination.size)
    psus.value = response.data.content
    psuPagination.total = response.data.totalElements
  } catch (error) {
    console.error('加载PSU列表失败:', error)
    ElMessage.error('加载PSU列表失败')
  }
}

// 加载版本审核列表
const loadVersionReviews = async () => {
  try {
    const response = await versionReviewApi.getVersionReviews(null, versionReviewPagination.page, versionReviewPagination.size)
    versionReviews.value = Array.isArray(response.data?.content) ? response.data.content : []
    versionReviewPagination.total = response.data?.totalElements || 0
  } catch (error) {
    console.error('加载版本审核列表失败:', error)
    versionReviews.value = []
  }
}

// 版本审核分页切换
const handleVersionReviewPageChange = (page) => {
  versionReviewPagination.page = page
  loadVersionReviews()
}

onMounted(() => {
  loadPsus()
  loadVersionReviews()
})
</script>

<style scoped>
.developer-dashboard {
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
  margin-bottom: 20px;
}

.code-preview {
  margin-top: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
}

.code-display {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 60vh;
  overflow-y: auto;
}

.data-display {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 60vh;
  overflow-y: auto;
}

.test-dataset-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #dcdfe6;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  margin: 0;
}

h2, h3 {
  margin-bottom: 20px;
}
</style>
