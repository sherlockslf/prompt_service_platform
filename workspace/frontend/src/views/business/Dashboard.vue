<template>
  <div class="business-dashboard">
    <el-container>
      <el-aside :width="sidebarWidth" class="sidebar" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
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
        <!-- PSU列表 -->
        <div v-if="activeMenu === '1'" class="content-section neo-panel">
          <div class="header-section">
            <h2>PSU列表</h2>
            <el-button type="primary" @click="showCreatePsuDialog = true">新建PSU</el-button>
          </div>
          <el-table class="table-neo" :data="psus" style="width: 100%; margin-top: 20px;" v-loading="loadingPsus">
            <el-table-column label="PSU ID" width="200">
              <template #default="{ row }">
                <el-button link type="primary" @click="goToPsuPreview(row)">{{ row.psuId }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" width="200"></el-table-column>
            <el-table-column prop="description" label="描述"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getPsuStatusTagType(row.status)">
                  {{ getPsuStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标签" width="100">
              <template #default="{ row }">
                <el-tag :type="getPsuTagType(row.tag)">{{ getPsuTagText(row.tag) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="360">
              <template #default="{ row }">
                <el-button size="small" @click="viewSchema(row)">查看Schema</el-button>
                <el-button size="small" @click="openEditPsuDialog(row)" :disabled="row.status !== 'DRAFT'">编辑</el-button>
                <el-button size="small" type="danger" @click="archivePsu(row)" :disabled="row.status === 'FORMAL'">归档</el-button>
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
        <div v-if="activeMenu === '2'" class="content-section neo-panel">
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
              <el-table class="table-neo" :data="schemaFields" style="width: 100%; margin-top: 10px;">
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
        <div v-if="activeMenu === '4'" class="content-section neo-panel">
          <h2>版本提交与跟进</h2>
          <el-form :model="versionForm" label-width="120px">
            <el-form-item label="选择PSU">
              <el-select v-model="selectedVersionPsuId" placeholder="请选择PSU" @change="onSelectedVersionPsuChange">
                <el-option 
                  v-for="psu in psus" 
                  :key="psu.id" 
                  :label="psu.name" 
                  :value="psu.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="选择版本">
              <el-select
                v-model="selectedSubmitReviewId"
                placeholder="请选择要提交/查看的版本"
                :disabled="!selectedVersionPsuId"
                style="width: 100%;"
              >
                <el-option
                  v-for="item in submitVersionOptions"
                  :key="item.versionNo"
                  :label="`v${item.versionNo} / ${getStatusText(item.status)} / 变更:${formatDateTime(item.createdAt)}`"
                  :value="item.versionNo"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="openSelectedSchemaSnapshot" :disabled="!selectedSubmitReviewId">查看JSON Schema详情</el-button>
              <el-button @click="openSelectedPromptSnapshot" :disabled="!selectedSubmitReviewId">查看Prompt详情</el-button>
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
            <el-table class="table-neo" :data="versionStatus" style="width: 100%;" v-loading="loadingVersions">
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
        <div v-if="activeMenu === '5'" class="content-section neo-panel">
          <h2>JSON Schema编辑器</h2>
          <el-form :model="schemaEditForm" label-width="120px">
            <el-form-item label="选择PSU">
              <el-select
                v-model="selectedSchemaPsuId"
                placeholder="请选择PSU"
                filterable
                remote
                reserve-keyword
                :remote-method="searchSchemaPsuByName"
                :loading="loadingSchemaPsuOptions"
                @change="loadSchemaForEdit"
              >
                <el-option
                  v-for="psu in schemaPsuOptions"
                  :key="psu.id"
                  :label="psu.name"
                  :value="psu.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="Schema内容">
              <div class="schema-param-editor">
                <div class="schema-param-header">
                  <span>按参数行编辑（参数名 / 类型 / 描述）</span>
                  <el-button size="small" @click="addSchemaParamRow">添加参数</el-button>
                </div>
                <el-table class="table-neo schema-param-table" :data="schemaParamRows" style="width: 100%">
                  <el-table-column label="参数名" min-width="240">
                    <template #default="{ row }">
                      <el-input v-model="row.name" placeholder="例如：userQuery" />
                    </template>
                  </el-table-column>
                  <el-table-column label="类型" width="180">
                    <template #default="{ row }">
                      <el-select v-model="row.type" placeholder="类型">
                        <el-option
                          v-for="type in schemaTypeOptions"
                          :key="type"
                          :label="type"
                          :value="type" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="描述" min-width="260">
                    <template #default="{ row }">
                      <el-input v-model="row.description" placeholder="参数说明（可选）" />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="100">
                    <template #default="{ $index }">
                      <el-button type="danger" link @click="removeSchemaParamRow($index)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
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
          <div class="test-dataset-section" v-if="selectedSchemaPsuId">
            <div class="header-section">
              <h3>测试数据</h3>
              <el-button type="primary" size="small" @click="openCreateDatasetDialog">添加测试数据</el-button>
            </div>
            <el-table class="table-neo" :data="testDatasets" style="width: 100%; margin-top: 10px;" v-loading="loadingDatasets">
              <el-table-column prop="id" label="ID" width="90"></el-table-column>
              <el-table-column prop="name" label="名称" min-width="180"></el-table-column>
              <el-table-column label="数据内容" min-width="220">
                <template #default="{ row }">
                  <el-button size="small" @click="viewDatasetData(row)">查看</el-button>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220">
                <template #default="{ row }">
                  <el-button size="small" @click="editDataset(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteDataset(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 版本审核 -->
        <div v-if="activeMenu === '6'" class="content-section neo-panel">
          <h2>版本审核</h2>
          <el-table class="table-neo" :data="versionStatus" style="width: 100%;" v-loading="loadingVersions">
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
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <template v-if="row.status === 'CANDIDATE'">
                  <el-button size="small" type="success" @click="approveVersion(row)">通过</el-button>
                  <el-button size="small" type="danger" @click="rejectVersion(row)">拒绝</el-button>
                </template>
                <span v-else>{{ getReviewResultText(row) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 代码生成 -->
        <div v-if="activeMenu === '7'" class="content-section neo-panel">
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
        <div v-if="activeMenu === '8'" class="content-section neo-panel">
          <ReleaseCenter />
        </div>

        <!-- 系统管理 -->
        <div v-if="activeMenu === '9'" class="content-section neo-panel">
          <h2>系统管理</h2>
          <el-alert title="系统管理能力已在当前页统一导航下接入，后续可按需继续补充子模块。" type="info" :closable="false" show-icon />
        </div>
      </el-main>
    </el-container>
    
    <!-- Schema查看对话框 -->
    <el-dialog v-model="showSchemaDialog" title="Schema详情" width="70%">
      <el-table class="table-neo" :data="viewingSchemaFields" style="width: 100%;">
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

    <el-dialog v-model="showCreateDatasetDialog" :title="editingDatasetId ? '编辑测试数据' : '添加测试数据'" width="700px">
      <el-form :model="datasetForm" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="datasetForm.name" placeholder="请输入测试数据名称"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="datasetForm.description" placeholder="请输入描述（可选）"></el-input>
        </el-form-item>
        <el-form-item label="数据内容">
          <div class="schema-param-editor">
            <div class="schema-param-header">
              <span>按参数赋值（参数名 / 参数值）</span>
            </div>
            <el-table class="table-neo schema-param-table" :data="datasetParamRows" style="width: 100%">
              <el-table-column label="参数名" min-width="240">
                <template #default="{ row }">
                  <span class="dataset-param-name">{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="参数值" min-width="340">
                <template #default="{ row }">
                  <div>
                    <el-input v-model="row.value" placeholder="例如：你好 / 123 / true / {&quot;k&quot;:&quot;v&quot;}" />
                    <div v-if="row.description" class="dataset-param-desc">{{ row.description }}</div>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDatasetDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDataset">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showViewDatasetDialog" title="测试数据详情" width="700px">
      <el-input v-model="viewingDatasetData" type="textarea" :rows="14" readonly></el-input>
    </el-dialog>

    <el-dialog v-model="showVersionSchemaDialog" title="JSON Schema快照详情" width="760px">
      <el-input v-model="selectedVersionSchemaSnapshot" type="textarea" :rows="18" readonly></el-input>
    </el-dialog>

    <el-dialog v-model="showVersionPromptDialog" title="Prompt快照详情" width="760px">
      <el-input v-model="selectedVersionPromptSnapshot" type="textarea" :rows="18" readonly></el-input>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { psuApi, schemaApi, promptApi, testDatasetApi, versionApi, versionReviewApi } from '@/services/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import ReleaseCenter from '@/views/developer/ReleaseCenter.vue'

const activeMenu = ref('1')
const route = useRoute()
const router = useRouter()
const isSidebarCollapsed = ref(false)
const showSchemaDialog = ref(false)
const showCreatePsuDialog = ref(false)
const showEditPsuDialog = ref(false)
const loadingPsus = ref(false)
const loadingVersions = ref(false)
const loadingDatasets = ref(false)
const showCreateDatasetDialog = ref(false)
const showViewDatasetDialog = ref(false)
const viewingDatasetData = ref('')
const editingDatasetId = ref(null)
const showVersionSchemaDialog = ref(false)
const showVersionPromptDialog = ref(false)
const selectedVersionSchemaSnapshot = ref('')
const selectedVersionPromptSnapshot = ref('')
const selectedSubmitReviewId = ref(null)

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
const schemaPsuOptions = ref([])
const loadingSchemaPsuOptions = ref(false)
let schemaPsuSearchDebounceTimer = null
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
const schemaTypeOptions = ['string', 'number', 'integer', 'boolean', 'array', 'object', 'null']
const schemaParamRows = ref([])
const testDatasets = ref([])
const datasetForm = reactive({
  name: '',
  description: '',
  dataContent: ''
})
const datasetParamRows = ref([])

// 版本相关
const versionForm = reactive({
  description: ''
})
const versionStatus = ref([])
const submitPsuVersions = ref([])
const submitVersionOptions = computed(() => {
  if (!selectedVersionPsuId.value) return []
  return submitPsuVersions.value
    // 后端接口已保证仅返回“可提交审核”的版本。
    .sort((a, b) => (Number(b.versionNo || 0) - Number(a.versionNo || 0)))
})
const generatedCodeInBusiness = ref('')

// 提交状态
const submitting = ref(false)

// Prompt编辑状态
const currentPromptEditable = ref(true)
const currentPromptFragmentId = ref(null)
const isBusinessUser = ref(true)

const goToPsuPreview = (psu) => {
  if (!psu?.id) return
  router.push({ path: '/business/psus/preview', query: { psuId: String(psu.id) } })
}

// 菜单选择处理
const handleMenuSelect = (index) => {
  // Prompt编排改为容器页面，避免继续使用旧的内嵌编辑区。
  if (index === '2') {
    router.push('/business/composer')
    return
  }
  // 其他模块仍在当前页面切换
  activeMenu.value = index
}

const sidebarWidth = computed(() => (isSidebarCollapsed.value ? '56px' : '200px'))

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

// 分页切换
const handlePageChange = (page) => {
  psuPagination.page = page
  loadPsus()
}

const loadSchemaPsuOptions = async (name = '') => {
  loadingSchemaPsuOptions.value = true
  try {
    const response = await psuApi.getPsus(1, 10, name)
    schemaPsuOptions.value = Array.isArray(response.data?.content) ? response.data.content : []
  } catch (error) {
    console.error('加载Schema可选PSU失败:', error)
    schemaPsuOptions.value = []
  } finally {
    loadingSchemaPsuOptions.value = false
  }
}

const searchSchemaPsuByName = (keyword) => {
  if (schemaPsuSearchDebounceTimer) {
    clearTimeout(schemaPsuSearchDebounceTimer)
  }
  schemaPsuSearchDebounceTimer = setTimeout(() => {
    loadSchemaPsuOptions(keyword || '')
  }, 250)
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

const createEmptySchemaParamRow = () => ({
  name: '',
  type: 'string',
  description: ''
})

const addSchemaParamRow = () => {
  schemaParamRows.value.push(createEmptySchemaParamRow())
}

const removeSchemaParamRow = (index) => {
  schemaParamRows.value.splice(index, 1)
  if (!schemaParamRows.value.length) {
    schemaParamRows.value.push(createEmptySchemaParamRow())
  }
}

const parseSchemaContentToRows = (schemaContent) => {
  let parsed = schemaContent
  if (typeof parsed === 'string') {
    parsed = parsed?.trim() ? JSON.parse(parsed) : {}
  }
  const fields = parseSchemaFields(parsed || {})
  if (!fields.length) {
    schemaParamRows.value = [createEmptySchemaParamRow()]
    return
  }
  schemaParamRows.value = fields.map((field) => ({
    name: field.name || '',
    type: field.type || 'string',
    description: field.description || ''
  }))
}

const buildSchemaContentFromRows = () => {
  const rows = schemaParamRows.value
    .map((row) => ({
      name: String(row.name || '').trim(),
      type: row.type || 'string',
      description: String(row.description || '').trim()
    }))
    .filter((row) => row.name)

  const duplicatedNames = rows
    .map((row) => row.name)
    .filter((name, index, arr) => arr.indexOf(name) !== index)
  if (duplicatedNames.length) {
    throw new Error(`参数名重复: ${[...new Set(duplicatedNames)].join(', ')}`)
  }

  const properties = {}
  rows.forEach((row) => {
    properties[row.name] = {
      type: row.type
    }
    if (row.description) {
      properties[row.name].description = row.description
    }
  })

  return JSON.stringify({
    type: 'object',
    properties
  })
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

const getReviewResultText = (review) => {
  if (review?.status === 'FORMAL') return '已审核通过'
  if (review?.status === 'ARCHIVED') {
    return review?.reviewedAt ? '已审核拒绝' : '已归档'
  }
  if (review?.status === 'DRAFT') return '草稿中'
  return getStatusText(review?.status)
}

const getReviewSortTimestamp = (review) => {
  const timeValue = review?.reviewedAt || review?.submittedAt || ''
  const timestamp = Date.parse(timeValue)
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const formatPrettyJson = (value) => {
  if (value == null || value === '') return '{}'
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
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

const getPsuTagType = (tag) => {
  switch (tag) {
    case 'FORMAL': return 'success'
    case 'PREVIEW': return 'warning'
    default: return 'info'
  }
}

const getPsuTagText = (tag) => {
  switch (tag) {
    case 'FORMAL': return '正式版'
    case 'PREVIEW': return '预览版'
    default: return '-'
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
  const psu = psus.value.find(item => item.id === selectedPsuId.value)
  if (!psu || psu.status === 'ARCHIVED') {
    ElMessage.warning('当前PSU状态不允许编辑Prompt')
    return
  }
  
  try {
    if (currentPromptFragmentId.value) {
      // 更新现有的Prompt片段
      await promptApi.updatePromptFragment(currentPromptFragmentId.value, {
        baseVersionNo: psu.versionNo,
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
    await loadPsus()
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
  const psu = psus.value.find(item => item.id === selectedPsuId.value)
  if (!psu || psu.status === 'ARCHIVED') {
    ElMessage.warning('当前PSU状态不允许定版')
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
    schemaParamRows.value = [createEmptySchemaParamRow()]
    testDatasets.value = []
    return
  }
  try {
    const response = await schemaApi.getSchema(selectedSchemaPsuId.value)
    schemaEditForm.schemaContent = response.data?.schemaContent || '{}'
    schemaEditForm.changeLog = response.data?.changeLog || ''
    parseSchemaContentToRows(schemaEditForm.schemaContent)
    await loadTestDatasets()
  } catch (error) {
    console.error('加载Schema失败:', error)
    schemaEditForm.schemaContent = '{}'
    schemaEditForm.changeLog = ''
    schemaParamRows.value = [createEmptySchemaParamRow()]
    testDatasets.value = []
    ElMessage.error('加载Schema失败')
  }
}

// 在当前业务页内保存Schema编辑数据
const saveSchemaFromEditor = async () => {
  if (!selectedSchemaPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  const psu = psus.value.find(item => item.id === selectedSchemaPsuId.value)
  if (!psu || psu.status === 'ARCHIVED') {
    ElMessage.warning('当前PSU状态不允许编辑Schema')
    return
  }
  try {
    schemaEditForm.schemaContent = buildSchemaContentFromRows()
    await schemaApi.updateSchema(selectedSchemaPsuId.value, {
      baseVersionNo: psu.versionNo,
      schemaContent: schemaEditForm.schemaContent,
      changeLog: schemaEditForm.changeLog
    })
    ElMessage.success('Schema保存成功')
    await loadPsus()
    await loadTestDatasets()
  } catch (error) {
    console.error('保存Schema失败:', error)
    ElMessage.error(error?.message || '保存Schema失败')
  }
}

const loadTestDatasets = async () => {
  if (!selectedSchemaPsuId.value) return
  loadingDatasets.value = true
  try {
    const response = await testDatasetApi.getTestDatasets(selectedSchemaPsuId.value)
    testDatasets.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    console.error('加载测试数据失败:', error)
    testDatasets.value = []
  } finally {
    loadingDatasets.value = false
  }
}

const createEmptyDatasetParamRow = () => ({
  name: '',
  value: '',
  description: ''
})

const buildDatasetRowsFromSchema = (dataContent) => {
  let parsed = dataContent
  try {
    parsed = typeof parsed === 'string' ? JSON.parse(parsed || '{}') : (parsed || {})
  } catch {
    parsed = {}
  }
  const obj = parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  const schemaRows = schemaParamRows.value.filter((row) => String(row.name || '').trim())
  datasetParamRows.value = schemaRows.map((row) => {
    const raw = obj[row.name]
    return {
      name: row.name,
      description: row.description || '',
      value: raw === undefined ? '' : (typeof raw === 'string' ? raw : JSON.stringify(raw))
    }
  })
}

const buildDatasetContentFromRows = () => {
  const rows = datasetParamRows.value
    .map((row) => ({
      rawValue: String(row.value ?? '').trim()
    }))

  const payload = {}
  datasetParamRows.value.forEach((row, idx) => {
    const fieldName = row.name
    const valueRow = rows[idx]
    if (!fieldName) return
    if (!valueRow?.rawValue) {
      payload[fieldName] = ''
      return
    }
    try {
      payload[fieldName] = JSON.parse(valueRow.rawValue)
    } catch {
      payload[fieldName] = valueRow.rawValue
    }
  })
  return JSON.stringify(payload)
}

const countFilledDatasetValues = () => {
  return datasetParamRows.value.filter((row) => String(row.value ?? '').trim() !== '').length
}

const ensureDatasetSchemaRowsReady = () => {
  if (!schemaParamRows.value.length) {
    throw new Error('当前Schema未定义可填写参数，请先维护Schema字段')
  }
  if (!datasetParamRows.value.length) {
    buildDatasetRowsFromSchema('{}')
  }
}

const resetDatasetForm = () => {
  editingDatasetId.value = null
  datasetForm.name = ''
  datasetForm.description = ''
  datasetForm.dataContent = ''
  datasetParamRows.value = []
}

const openCreateDatasetDialog = () => {
  resetDatasetForm()
  try {
    ensureDatasetSchemaRowsReady()
  } catch (error) {
    ElMessage.warning(error.message || '请先维护Schema字段')
    return
  }
  showCreateDatasetDialog.value = true
}

const parseDatasetContentToRows = (dataContent) => {
  buildDatasetRowsFromSchema(dataContent)
}

const saveDataset = async () => {
  if (!selectedSchemaPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  if (!datasetForm.name) {
    ElMessage.warning('请填写名称')
    return
  }
  try {
    ensureDatasetSchemaRowsReady()
    if (!countFilledDatasetValues()) {
      ElMessage.warning('请至少填写一个参数值')
      return
    }
    datasetForm.dataContent = buildDatasetContentFromRows()
    if (editingDatasetId.value) {
      await testDatasetApi.updateTestDataset(editingDatasetId.value, {
        name: datasetForm.name,
        description: datasetForm.description,
        dataContent: datasetForm.dataContent
      })
      ElMessage.success('测试数据更新成功')
    } else {
      await testDatasetApi.createTestDataset(selectedSchemaPsuId.value, {
        name: datasetForm.name,
        description: datasetForm.description,
        dataContent: datasetForm.dataContent
      })
      ElMessage.success('测试数据创建成功')
    }
    showCreateDatasetDialog.value = false
    resetDatasetForm()
    await loadTestDatasets()
  } catch (error) {
    console.error('保存测试数据失败:', error)
    ElMessage.error('保存测试数据失败')
  }
}

const editDataset = (dataset) => {
  editingDatasetId.value = dataset.id
  datasetForm.name = dataset.name || ''
  datasetForm.description = dataset.description || ''
  datasetForm.dataContent = dataset.dataContent || '{}'
  try {
    ensureDatasetSchemaRowsReady()
    parseDatasetContentToRows(datasetForm.dataContent)
  } catch (error) {
    ElMessage.warning(error.message || '请先维护Schema字段')
    return
  }
  showCreateDatasetDialog.value = true
}

const deleteDataset = async (dataset) => {
  try {
    await ElMessageBox.confirm(`确定删除测试数据 "${dataset.name}" 吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await testDatasetApi.deleteTestDataset(dataset.id)
    ElMessage.success('测试数据已删除')
    await loadTestDatasets()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除测试数据失败:', error)
      ElMessage.error('删除测试数据失败')
    }
  }
}

const viewDatasetData = (dataset) => {
  viewingDatasetData.value = dataset.dataContent || ''
  showViewDatasetDialog.value = true
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
  const psu = psus.value.find(item => item.id === selectedVersionPsuId.value)
  if (!psu || psu.status === 'ARCHIVED' || psu.status === 'CANDIDATE') {
    ElMessage.warning('当前状态不允许提交审核')
    return
  }
  
  submitting.value = true
  try {
    if (selectedSubmitReviewId.value) {
      const selectedReview = versionStatus.value.find(
        item => item.psuId === selectedVersionPsuId.value && item.versionNo === selectedSubmitReviewId.value
      )
      if (selectedReview && selectedReview.status !== 'CANDIDATE') {
        ElMessage.warning('当前选中版本已存在审核结果，请选择候选版本或重新创建候选版本')
        submitting.value = false
        return
      }
    }
    await versionApi.submitVersion(selectedVersionPsuId.value, selectedSubmitReviewId.value)
    ElMessage.success('版本提交成功，等待研发审核')
    submitting.value = false
    versionForm.description = ''
    await loadVersionStatus()
    await onSelectedVersionPsuChange(selectedVersionPsuId.value)
  } catch (error) {
    console.error('提交版本失败:', error)
    ElMessage.error('提交失败')
    submitting.value = false
  }
}

const onSelectedVersionPsuChange = async (psuId) => {
  const pid = Number(psuId)
  if (!Number.isInteger(pid) || pid <= 0) {
    submitPsuVersions.value = []
    selectedSubmitReviewId.value = null
    return
  }
  const psu = psus.value.find(item => item.id === pid)
  if (!psu?.psuId) {
    submitPsuVersions.value = []
    selectedSubmitReviewId.value = null
    return
  }
  try {
    const response = await psuApi.getSubmittablePsuVersions(psu.psuId)
    submitPsuVersions.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    console.error('加载PSU版本历史失败:', error)
    submitPsuVersions.value = []
    ElMessage.error('加载PSU版本历史失败')
  }
  const firstSubmittable = submitVersionOptions.value[0]
  selectedSubmitReviewId.value = firstSubmittable?.versionNo || null
}

const openSelectedSchemaSnapshot = async () => {
  if (!selectedSubmitReviewId.value) {
    ElMessage.warning('请先选择版本')
    return
  }
  try {
    const review = versionStatus.value.find(
      item => item.psuId === selectedVersionPsuId.value && item.versionNo === selectedSubmitReviewId.value
    )
    if (!review?.id) {
      ElMessage.warning('该版本尚未进入审核，暂无审核快照可查看')
      return
    }
    const response = await versionReviewApi.getReviewSnapshot(review.id)
    const snapshot = response.data || {}
    selectedVersionSchemaSnapshot.value = formatPrettyJson(snapshot.specJsonSnapshot)
    showVersionSchemaDialog.value = true
  } catch (error) {
    console.error('加载Schema快照失败:', error)
    ElMessage.error(error.response?.data?.message || '加载Schema快照失败')
  }
}

const openSelectedPromptSnapshot = async () => {
  if (!selectedSubmitReviewId.value) {
    ElMessage.warning('请先选择版本')
    return
  }
  try {
    const review = versionStatus.value.find(
      item => item.psuId === selectedVersionPsuId.value && item.versionNo === selectedSubmitReviewId.value
    )
    if (!review?.id) {
      ElMessage.warning('该版本尚未进入审核，暂无审核快照可查看')
      return
    }
    const response = await versionReviewApi.getReviewSnapshot(review.id)
    const snapshot = response.data || {}
    selectedVersionPromptSnapshot.value = snapshot.promptContentSnapshot || ''
    showVersionPromptDialog.value = true
  } catch (error) {
    console.error('加载Prompt快照失败:', error)
    ElMessage.error(error.response?.data?.message || '加载Prompt快照失败')
  }
}

const approveVersion = async (review) => {
  try {
    await ElMessageBox.confirm(
      `确认通过 PSU ${review.psuId} 的版本 ${review.versionNo} 吗？`,
      '版本审核',
      {
        confirmButtonText: '通过',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await versionReviewApi.reviewVersion(review.id, {
      approved: true
    })
    ElMessage.success('版本审核通过')
    await loadVersionStatus()
    await loadPsus()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核通过失败:', error)
      ElMessage.error(error.response?.data?.message || '审核通过失败')
    }
  }
}

const rejectVersion = async (review) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入拒绝原因（PSU ${review.psuId} / 版本 ${review.versionNo}）`,
      '拒绝审核',
      {
        confirmButtonText: '提交拒绝',
        cancelButtonText: '取消',
        inputPattern: /^.{2,500}$/,
        inputErrorMessage: '拒绝原因至少2个字符'
      }
    )
    await versionReviewApi.reviewVersion(review.id, {
      approved: false,
      rejectionReason: value,
      rejectionType: 'BACK_TO_BIZ'
    })
    ElMessage.success('版本已拒绝')
    await loadVersionStatus()
    await loadPsus()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拒绝审核失败:', error)
      ElMessage.error(error.response?.data?.message || '拒绝审核失败')
    }
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
    const rawList = Array.isArray(response.data)
      ? response.data
      : (Array.isArray(response.data?.content) ? response.data.content : [])
    versionStatus.value = [...rawList].sort((a, b) => getReviewSortTimestamp(b) - getReviewSortTimestamp(a))
    if (selectedVersionPsuId.value) {
      await onSelectedVersionPsuChange(selectedVersionPsuId.value)
    }
  } catch (error) {
    console.error('加载版本状态失败:', error)
    versionStatus.value = []
  } finally {
    loadingVersions.value = false
  }
}

onMounted(() => {
  schemaParamRows.value = [createEmptySchemaParamRow()]
  datasetParamRows.value = []
  loadSchemaPsuOptions()
  const menu = route.query.menu
  const queryPsuId = Number(route.query.psuId)
  if (menu === '5') {
    activeMenu.value = '5'
    if (Number.isInteger(queryPsuId) && queryPsuId > 0) {
      selectedSchemaPsuId.value = queryPsuId
      loadSchemaForEdit()
    }
  }
  loadPsus()
  loadVersionStatus()
})

onBeforeUnmount(() => {
  if (schemaPsuSearchDebounceTimer) {
    clearTimeout(schemaPsuSearchDebounceTimer)
  }
})
</script>

<style scoped>
.business-dashboard {
  --layout-header-height: 72px;
  --layout-sidebar-offset: 16px;
  min-height: calc(100vh - var(--layout-header-height));
  color: var(--neo-text);
}

.sidebar {
  background: linear-gradient(180deg, rgba(9, 23, 41, 0.9), rgba(7, 14, 28, 0.95));
  color: var(--neo-text);
  border-right: 1px solid var(--neo-border);
  border-radius: 0 16px 16px 0;
  box-shadow: 12px 0 24px rgba(0, 0, 0, 0.24);
  height: calc(100vh - var(--layout-header-height) - var(--layout-sidebar-offset));
  position: fixed;
  top: calc(var(--layout-header-height) + var(--layout-sidebar-offset));
  left: 0;
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-collapsed {
  overflow: visible;
}

.menu {
  border-right: none;
  background: transparent;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

:deep(.menu .el-menu-item) {
  color: var(--neo-text-dim);
  border-left: 2px solid transparent;
  margin: 6px 10px;
  border-radius: 10px;
  justify-content: center;
  text-align: center;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

:deep(.menu .el-menu-item:hover) {
  color: #d8f8ff;
  background: rgba(77, 231, 200, 0.12);
}

:deep(.menu .el-menu-item.is-active) {
  color: #dbfffb;
  background: linear-gradient(90deg, rgba(77, 231, 200, 0.24), rgba(87, 168, 255, 0.16));
  border-left-color: var(--neo-accent);
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
  top: 50%;
  right: -14px;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border: 1px solid var(--neo-border);
  border-radius: 14px;
  background: rgba(10, 24, 43, 0.96);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.22);
}

.triangle-left {
  width: 0;
  height: 0;
  border-top: 7px solid transparent;
  border-bottom: 7px solid transparent;
  border-right: 10px solid #94e9dc;
}

.triangle-right {
  width: 0;
  height: 0;
  border-top: 7px solid transparent;
  border-bottom: 7px solid transparent;
  border-left: 10px solid #94e9dc;
}

.main-content {
  margin-left: 200px;
  min-height: calc(100vh - var(--layout-header-height));
  padding: 22px;
  overflow-y: auto;
}

.main-content-collapsed {
  margin-left: 56px;
}

.content-section {
  padding: 20px;
}

.neo-panel {
  border: 1px solid var(--neo-border);
  border-radius: var(--neo-radius-lg);
  background: var(--neo-surface);
  box-shadow: var(--neo-shadow);
  backdrop-filter: blur(8px);
  animation: panel-in 240ms ease-out;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.prompt-editor-container {
  display: flex;
  gap: 20px;
}

.variable-panel {
  width: 200px;
  border: 1px solid var(--neo-border);
  border-radius: var(--neo-radius-md);
  padding: 10px;
  background-color: var(--neo-surface-soft);
}

.variable-list {
  margin-top: 10px;
}

.variable-tag {
  margin: 5px;
  cursor: pointer;
}

.schema-param-editor {
  width: 100%;
  border: 1px solid var(--neo-border);
  border-radius: var(--neo-radius-md);
  background: rgba(7, 19, 35, 0.7);
  padding: 10px;
}

.schema-param-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--neo-text-dim);
  font-size: 13px;
  margin-bottom: 8px;
}

.schema-param-table {
  border-radius: var(--neo-radius-sm);
}

.dataset-param-name {
  color: var(--neo-text);
  font-weight: 600;
}

.dataset-param-desc {
  margin-top: 6px;
  font-size: 12px;
  color: var(--neo-text-dim);
  line-height: 1.4;
}

.input-field {
  margin-bottom: 15px;
}

.input-field label {
  display: block;
  margin-bottom: 5px;
  font-weight: 600;
}

.version-status {
  margin-top: 30px;
}

h2, h3 {
  margin-bottom: 20px;
  color: var(--neo-text);
}

:deep(.el-button--primary) {
  border: none !important;
  background: linear-gradient(110deg, var(--neo-accent), var(--neo-accent-2)) !important;
  color: #071120 !important;
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(77, 231, 200, 0.28);
}

:deep(.el-button--danger) {
  border: none !important;
  background: linear-gradient(110deg, #ff7c95, var(--neo-danger)) !important;
  color: #1a0810 !important;
  font-weight: 700;
}

:deep(.el-button.is-link) {
  color: #88f9e5 !important;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper) {
  background: rgba(6, 16, 30, 0.88) !important;
  box-shadow: inset 0 0 0 1px var(--neo-border) !important;
  color: var(--neo-text) !important;
}

:deep(.el-textarea__inner) {
  color: var(--neo-text) !important;
}

:deep(.el-pagination) {
  color: var(--neo-text-dim);
}

:deep(.table-neo) {
  border-radius: var(--neo-radius-md);
  overflow: hidden;
}

:deep(.table-neo .el-table),
:deep(.table-neo .el-table__inner-wrapper),
:deep(.table-neo .el-table tr),
:deep(.table-neo .el-table th.el-table__cell),
:deep(.table-neo .el-table td.el-table__cell) {
  background: transparent !important;
}

:deep(.table-neo .el-table th.el-table__cell) {
  color: #b9d8eb;
  border-bottom: 1px solid rgba(95, 158, 199, 0.22);
}

:deep(.table-neo .el-table td.el-table__cell) {
  color: var(--neo-text);
  border-bottom: 1px solid rgba(95, 158, 199, 0.12);
}

:deep(.table-neo .el-table__row:hover td.el-table__cell) {
  background: rgba(77, 231, 200, 0.08) !important;
}

@keyframes panel-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 992px) {
  .main-content {
    padding: 14px;
  }

  .content-section {
    padding: 14px;
  }
}
</style>
