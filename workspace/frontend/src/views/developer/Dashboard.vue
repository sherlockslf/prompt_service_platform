<template>
  <div class="developer-dashboard">
    <el-container>
      <el-aside :width="sidebarWidth" class="sidebar" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
        <el-menu :default-active="activeMenu" class="menu" @select="handleMenuSelect">
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
          <el-menu-item index="7">
            <span>评估中心</span>
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
            <el-table-column prop="updatedAt" label="更新时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="editPsu(row)" :disabled="row.status !== 'DRAFT'">编辑</el-button>
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
              <div class="schema-param-editor">
                <div class="schema-param-header">
                  <span>按参数行编辑（参数名 / 类型 / 描述）</span>
                  <el-button size="small" @click="addSchemaParamRow">添加参数</el-button>
                </div>
                <el-table :data="schemaParamRows" style="width: 100%">
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
                v-model="schemaForm.changeLog" 
                type="textarea" 
                :rows="3" 
                placeholder="请输入变更日志">
              </el-input>
            </el-form-item>
            <el-form-item label="参数集内容">
              <el-input
                v-model="paramSetForm.paramSetContent"
                type="textarea"
                :rows="8"
                placeholder="请输入参数集JSON（覆盖写）">
              </el-input>
            </el-form-item>
            <el-form-item label="参数集日志">
              <el-input
                v-model="paramSetForm.changeLog"
                type="textarea"
                :rows="2"
                placeholder="请输入参数集变更日志">
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSchema">保存Schema</el-button>
              <el-button type="primary" plain @click="saveParamSet">保存参数集</el-button>
              <el-button @click="loadSchema">加载最新版本</el-button>
              <el-button type="info" plain @click="openSchemaHistory">版本历史</el-button>
            </el-form-item>
          </el-form>

          <el-drawer
            v-model="showSchemaHistoryDrawer"
            title="Schema版本历史"
            size="55%"
            destroy-on-close
          >
            <el-table :data="schemaVersions" v-loading="loadingSchemaVersions" style="width: 100%">
              <el-table-column prop="version" label="版本" width="90" />
              <el-table-column prop="modifiedBy" label="修改人ID" width="110" />
              <el-table-column prop="modifierName" label="修改人" width="130">
                <template #default="{ row }">
                  {{ row.modifierName || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="更新时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt || row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column prop="changeLog" label="变更日志" min-width="220">
                <template #default="{ row }">
                  {{ row.changeLog || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="Schema快照" width="100">
                <template #default="{ row }">
                  <el-button size="small" @click="viewSchemaSnapshot(row)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-drawer>
          
          <!-- 测试数据集管理 -->
          <div class="test-dataset-section" v-if="selectedPsuId">
            <div class="section-header">
              <h3>测试数据集</h3>
              <el-button type="primary" size="small" @click="openCreateDatasetDialog">新建数据集</el-button>
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
              <el-table-column label="操作" width="280">
                <template #default="{ row }">
                  <el-button size="small" type="primary" plain @click="startDatasetEvaluation(row)">发起评估</el-button>
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
          <el-table :data="versionReviews" :row-class-name="versionReviewRowClassName" style="width: 100%;">
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
            <el-table-column label="版本标签" width="120">
              <template #default="{ row }">
                <el-tag :type="getPsuTagType(row.versionTag)">{{ getPsuTagText(row.versionTag) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitterId" label="提交者" width="120"></el-table-column>
            <el-table-column prop="submittedAt" label="提交时间" width="180"></el-table-column>
            <el-table-column prop="gitCommitHash" label="Git提交Hash" width="180">
              <template #default="{ row }">
                <span>{{ row.gitCommitHash || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="430">
              <template #default="{ row }">
                <el-button size="small" @click="goReviewPreview(row)" :disabled="row.status !== 'CANDIDATE'">预览</el-button>
                <el-button size="small" @click="approveVersion(row)" :disabled="row.status !== 'CANDIDATE'">通过</el-button>
                <el-button size="small" type="danger" @click="rejectVersion(row)" :disabled="row.status !== 'CANDIDATE'">拒绝</el-button>
                <el-button size="small" type="success" @click="registerGitCommit(row)" :disabled="row.status !== 'FORMAL'">登记Git</el-button>
                <el-button size="small" type="success" plain @click="setVersionTag(row, 'FORMAL')">设为正式</el-button>
                <el-button size="small" type="info" plain @click="setVersionTag(row, 'PREVIEW')">设为预览</el-button>
                <el-button size="small" @click="compareVersion(row)">对比</el-button>
                <el-button size="small" type="warning" @click="rollbackVersion(row)">回滚</el-button>
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
            <el-form-item label="代码语言">
              <el-select v-model="codeGenForm.language" placeholder="请选择代码语言">
                <el-option label="Java" value="java" />
                <el-option label="Python" value="python" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="generateCode">生成代码</el-button>
              <el-button @click="downloadCode">下载代码</el-button>
              <el-button type="success" plain @click="copySampleCallCode">复制最小调用示例</el-button>
            </el-form-item>
          </el-form>
          <el-descriptions v-if="generatedCode" :column="2" border style="margin-top: 12px;">
            <el-descriptions-item label="PSU">{{ selectedCodeGenPsuId }}</el-descriptions-item>
            <el-descriptions-item label="语言">{{ codeGenForm.language }}</el-descriptions-item>
            <el-descriptions-item label="文件名">{{ buildCodeFileName() }}</el-descriptions-item>
            <el-descriptions-item label="生成时间">{{ formatDateTime(new Date()) }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="generatedCode" class="code-preview">
            <h3>生成的代码预览</h3>
            <pre>{{ generatedCode }}</pre>
          </div>
          <div class="code-preview">
            <h3>最小调用示例</h3>
            <pre>{{ buildSampleCallCode() }}</pre>
          </div>
        </div>

        <!-- 发版中心 -->
        <div v-if="activeMenu === '6'" class="content-section">
          <ReleaseCenter />
        </div>
        
        <!-- 评估中心 -->
        <div v-if="activeMenu === '7'" class="content-section">
          <EvaluationCenter />
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
          <div class="dataset-param-editor">
            <div class="dataset-param-header">
              <span>按参数赋值（参数名 / 参数值）</span>
            </div>
            <el-table :data="datasetParamRows" style="width: 100%">
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
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { psuApi, schemaApi, testDatasetApi, versionReviewApi, paramSetApi } from '@/services/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import ReleaseCenter from '@/views/developer/ReleaseCenter.vue'
import EvaluationCenter from '@/views/developer/EvaluationCenter.vue'

const route = useRoute()
const router = useRouter()
const activeMenu = ref('1')
const isSidebarCollapsed = ref(false)
const showCreatePsuDialog = ref(false)
const showEditPsuDialog = ref(false)
const showEditPromptDialog = ref(false)
const showCodeDialog = ref(false)
const showCreateDatasetDialog = ref(false)
const showViewDatasetDialog = ref(false)
const showSchemaHistoryDrawer = ref(false)

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
const schemaTypeOptions = ['string', 'number', 'integer', 'boolean', 'array', 'object', 'null']
const schemaParamRows = ref([])
const paramSetForm = reactive({
  paramSetContent: '{}',
  changeLog: ''
})
const schemaVersions = ref([])
const loadingSchemaVersions = ref(false)

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
const codeGenForm = reactive({
  language: 'java'
})

// 测试数据集数据
const testDatasets = ref([])
const loadingDatasets = ref(false)
const editingDatasetId = ref(null)
const datasetForm = reactive({
  name: '',
  description: '',
  dataContent: ''
})
const datasetParamRows = ref([])
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
  // 统一在当前页面切换模块，避免左侧导航栏因跨路由跳转发生变化
  if (index === '7') {
    router.push('/developer?menu=7')
  }
  activeMenu.value = index
}

const sidebarWidth = computed(() => (isSidebarCollapsed.value ? '56px' : '200px'))

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

// 根据路由参数初始化当前菜单，支持从其他页面直达指定模块
const syncMenuFromRoute = () => {
  const routeMenu = String(route.query.menu || '')
  const allowedMenus = ['1', '2', '3', '4', '5', '6', '7']
  if (allowedMenus.includes(routeMenu)) {
    activeMenu.value = routeMenu
  }
}

const compareVersion = async (review) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入对比起始版本号', '版本对比', {
      confirmButtonText: '对比',
      cancelButtonText: '取消',
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入正整数版本号'
    })
    const fromVersionNo = Number(value)
    const res = await versionReviewApi.compareVersions(review.psuId, fromVersionNo, review.versionNo)
    const data = res.data || {}
    await ElMessageBox.alert(
      `对比版本: v${data.fromVersionNo} -> v${data.toVersionNo}\n行数变化: ${data.fromLineCount} -> ${data.toLineCount}\n新增行: ${data.addedLineCount}\n删除行: ${data.removedLineCount}\n是否有变更: ${data.changed ? '是' : '否'}`,
      '版本对比结果',
      { confirmButtonText: '关闭' }
    )
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '版本对比失败')
    }
  }
}

const rollbackVersion = async (review) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入回滚目标版本号', '版本回滚', {
      confirmButtonText: '回滚',
      cancelButtonText: '取消',
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入正整数版本号'
    })
    await versionReviewApi.rollbackVersion(review.psuId, {
      targetVersionNo: Number(value),
      reason: `manual-rollback-from-v${review.versionNo}`
    })
    ElMessage.success('版本回滚成功')
    await loadVersionReviews()
    await loadPsus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '版本回滚失败')
    }
  }
}

// 跳转审核预览页面，便于在审核前查看参数集渲染效果
const goReviewPreview = (review) => {
  router.push({ path: '/developer/psus/reviews', query: { psuId: String(review.psuId), reviewId: String(review.id) } })
}

const focusedReviewId = computed(() => {
  const id = Number(route.query.reviewId)
  return Number.isInteger(id) && id > 0 ? id : null
})

const queryPsuId = computed(() => {
  const id = Number(route.query.psuId)
  return Number.isInteger(id) && id > 0 ? id : null
})

const versionReviewRowClassName = ({ row }) => {
  // 从预览页返回时高亮当前审核单据，方便快速定位
  if (focusedReviewId.value && Number(row?.id) === focusedReviewId.value) {
    return 'focused-review-row'
  }
  return ''
}

// 加载参数集（覆盖写）
const loadParamSet = async () => {
  if (!selectedPsuId.value) return
  try {
    const response = await paramSetApi.getParamSet(selectedPsuId.value)
    paramSetForm.paramSetContent = formatJsonText(response.data?.paramSetContent || '{}')
    paramSetForm.changeLog = response.data?.changeLog || ''
  } catch (error) {
    paramSetForm.paramSetContent = '{}'
    paramSetForm.changeLog = ''
  }
}

// 保存参数集（覆盖写）
const saveParamSet = async () => {
  if (!selectedPsuId.value) return
  if (!isPsuDraft(selectedPsuId.value)) {
    ElMessage.warning('仅草稿状态允许编辑参数集')
    return
  }
  try {
    const parsed = JSON.parse(paramSetForm.paramSetContent || '{}')
    await paramSetApi.updateParamSet(selectedPsuId.value, {
      paramSetContent: JSON.stringify(parsed),
      changeLog: paramSetForm.changeLog
    })
    ElMessage.success('参数集保存成功')
  } catch (error) {
    console.error('保存参数集失败:', error)
    ElMessage.error(error.response?.data?.message || '保存参数集失败，请检查JSON格式')
  }
}

const formatJsonText = (data) => {
  if (!data) return '{}'
  try {
    const parsed = typeof data === 'string' ? JSON.parse(data) : data
    return JSON.stringify(parsed, null, 2)
  } catch {
    return String(data)
  }
}

const getPsuById = (id) => psus.value.find(item => item.id === id)

const isPsuDraft = (id) => getPsuById(id)?.status === 'DRAFT'

// 获取状态标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case 'CANDIDATE': return 'warning'
    case 'FORMAL': return 'success'
    case 'ARCHIVED': return 'info'
    case 'DRAFT': return ''
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'CANDIDATE': return '候选'
    case 'FORMAL': return '正式'
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

const setVersionTag = async (review, tag) => {
  try {
    await versionReviewApi.assignVersionTag(review.id, { tag })
    ElMessage.success(tag === 'FORMAL' ? '已设为正式版' : '已设为预览版')
    await loadVersionReviews()
    await loadPsus()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '设置版本标签失败')
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

const inferSchemaType = (value) => {
  if (value === null || value === undefined) return 'null'
  if (Array.isArray(value)) return 'array'
  const t = typeof value
  if (t === 'number') return 'number'
  if (t === 'boolean') return 'boolean'
  if (t === 'object') return 'object'
  return 'string'
}

const parseSchemaContentToRows = (schemaContent) => {
  let parsed = schemaContent
  if (typeof parsed === 'string') {
    parsed = parsed?.trim() ? JSON.parse(parsed) : {}
  }
  const schemaObj = parsed || {}
  const rows = []
  if (schemaObj.properties && typeof schemaObj.properties === 'object') {
    Object.entries(schemaObj.properties).forEach(([name, prop]) => {
      rows.push({
        name,
        type: prop?.type || 'string',
        description: prop?.description || ''
      })
    })
  } else if (typeof schemaObj === 'object' && !Array.isArray(schemaObj)) {
    Object.entries(schemaObj).forEach(([name, value]) => {
      rows.push({
        name,
        type: inferSchemaType(value),
        description: ''
      })
    })
  }
  schemaParamRows.value = rows.length ? rows : [createEmptySchemaParamRow()]
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
    properties[row.name] = { type: row.type }
    if (row.description) {
      properties[row.name].description = row.description
    }
  })

  return JSON.stringify({
    type: 'object',
    properties
  })
}

// 保存Schema
const saveSchema = async () => {
  const psu = getPsuById(selectedPsuId.value)
  if (!psu) {
    ElMessage.warning('请先选择PSU')
    return
  }
  try {
    schemaForm.schemaContent = buildSchemaContentFromRows()
    await api.post('/schemas/by-psuId', {
      baseVersionNo: psu.versionNo,
      schemaContent: schemaForm.schemaContent,
      changeLog: schemaForm.changeLog
    }, { params: { psuId: selectedPsuId.value } })
    ElMessage.success('Schema保存成功')
    await loadPsus()
  } catch (error) {
    console.error('保存Schema失败:', error)
    ElMessage.error(error?.message || '保存Schema失败')
  }
}

// 加载Schema
const loadSchema = async () => {
  if (!selectedPsuId.value) {
    schemaParamRows.value = [createEmptySchemaParamRow()]
    return
  }
  
  try {
    const response = await schemaApi.getSchema(selectedPsuId.value)
    schemaForm.schemaContent = response.data.schemaContent
    schemaForm.changeLog = response.data.changeLog || ''
    parseSchemaContentToRows(schemaForm.schemaContent)
    await loadParamSet()
    await loadSchemaVersions()
    // 加载Schema后同时加载测试数据集
    loadTestDatasets()
  } catch (error) {
    console.error('加载Schema失败:', error)
    schemaForm.schemaContent = '{}'
    schemaForm.changeLog = ''
    schemaParamRows.value = [createEmptySchemaParamRow()]
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
    if (!fieldName) {
      return
    }
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

const buildDatasetPayloadOrThrow = () => {
  ensureDatasetSchemaRowsReady()
  if (!countFilledDatasetValues()) {
    throw new Error('请至少填写一个参数值')
  }
  return buildDatasetContentFromRows()
}

// 保存测试数据集
const saveDataset = async () => {
  if (!datasetForm.name) {
    ElMessage.warning('请填写数据集名称')
    return
  }
  
  try {
    datasetForm.dataContent = buildDatasetPayloadOrThrow()
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
  datasetParamRows.value = []
}

// 加载Prompt片段
const loadPromptFragments = async () => {
  if (!selectedPromptPsuId.value) return
  
  try {
    const response = await api.get('/prompts/by-psuId', { params: { psuId: selectedPromptPsuId.value } })
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
  const psu = getPsuById(selectedPromptPsuId.value)
  if (!psu) {
    ElMessage.warning('请先选择PSU')
    return
  }
  try {
    await api.post('/prompts/by-fragmentId', {
      baseVersionNo: psu.versionNo,
      content: editingPromptFragment.value.content
    }, { params: { fragmentId: editingPromptFragment.value.id } })
    ElMessage.success('Prompt片段保存成功')
    showEditPromptDialog.value = false
    await loadPsus()
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
      rejectionReason: '不符合要求',
      rejectionType: 'BACK_TO_BIZ'
    })
    ElMessage.success('版本审核已拒绝')
    loadVersionReviews()
  } catch (error) {
    console.error('审核版本失败:', error)
    ElMessage.error('审核版本失败')
  }
}

// 登记Git提交哈希，便于版本与代码仓库记录对齐
const registerGitCommit = async (review) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入Git提交哈希（7-40位十六进制）', '登记Git提交', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPattern: /^[0-9a-fA-F]{7,40}$/,
      inputErrorMessage: '请输入7-40位十六进制hash'
    })
    await versionReviewApi.registerGitCommit(review.id, { gitCommitHash: value })
    ElMessage.success('Git提交登记成功')
    await loadVersionReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || 'Git提交登记失败')
    }
  }
}

const startDatasetEvaluation = (dataset) => {
  // 从测试集一键进入评估中心，并带上预选参数触发快速建任务。
  if (!selectedPsuId.value || !dataset?.id) {
    ElMessage.warning('请选择有效测试集后再发起评估')
    return
  }
  router.push({
    path: '/developer/evaluations',
    query: {
      psuId: String(selectedPsuId.value),
      datasetId: String(dataset.id),
      autoCreate: '1'
    }
  })
}

const loadSchemaVersions = async () => {
  if (!selectedPsuId.value) {
    schemaVersions.value = []
    return
  }
  loadingSchemaVersions.value = true
  try {
    const response = await schemaApi.getSchemaVersions(selectedPsuId.value)
    schemaVersions.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    schemaVersions.value = []
  } finally {
    loadingSchemaVersions.value = false
  }
}

const openSchemaHistory = async () => {
  if (!selectedPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  await loadSchemaVersions()
  showSchemaHistoryDrawer.value = true
}

const viewSchemaSnapshot = (schema) => {
  ElMessageBox.alert(formatJsonText(schema?.schemaContent || '{}'), 'Schema快照', {
    confirmButtonText: '关闭'
  })
}

// 查看代码
const viewCode = async (review) => {
  try {
    // 审核列表“查看代码”保持Java默认产物，避免影响既有使用习惯。
    const response = await versionReviewApi.getCode(review.psuId, 'java')
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
    const response = await versionReviewApi.getCode(selectedCodeGenPsuId.value, codeGenForm.language)
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
  a.download = buildCodeFileName()
  a.click()
  window.URL.revokeObjectURL(url)
}

const buildCodeFileName = () => {
  // 下载名统一为 PSU + 语言 + 时间戳，方便追溯产物来源。
  const lang = codeGenForm.language === 'python' ? 'python' : 'java'
  const ext = lang === 'python' ? 'py' : 'java'
  const ts = new Date()
    .toISOString()
    .replace(/[-:]/g, '')
    .replace('T', '-')
    .slice(0, 15)
  return `psu-${selectedCodeGenPsuId.value || 'unknown'}-${lang}-${ts}.${ext}`
}

const buildSampleCallCode = () => {
  const psuId = Number(selectedCodeGenPsuId.value || 0)
  const javaPsuLiteral = psuId > 0 ? `${psuId}L` : '1L // TODO 替换为真实PSU ID'
  const pyPsuLiteral = psuId > 0 ? String(psuId) : '1  # TODO 替换为真实PSU ID'
  if (codeGenForm.language === 'python') {
    return [
      'import requests',
      '',
      'payload = {',
      `  "psuId": ${pyPsuLiteral},`,
      '  "environment": "PROD",',
      '  "input": {"message": "hello"}',
      '}',
      'resp = requests.post("http://localhost:8084/api/prompt-service/resolve", json=payload, timeout=10)',
      'resp.raise_for_status()',
      'print(resp.json())'
    ].join('\n')
  }
  return [
    'Map<String, Object> payload = new HashMap<>();',
    `payload.put("psuId", ${javaPsuLiteral});`,
    'payload.put("environment", "PROD");',
    'payload.put("input", Map.of("message", "hello"));',
    '',
    'ResponseEntity<Map> resp = restTemplate.postForEntity(',
    '    "http://localhost:8084/api/prompt-service/resolve",',
    '    payload,',
    '    Map.class',
    ');',
    'System.out.println(resp.getBody());'
  ].join('\n')
}

// 复制代码
const copyCode = () => {
  navigator.clipboard.writeText(viewingCode.value)
  ElMessage.success('代码已复制到剪贴板')
}

const copySampleCallCode = async () => {
  // 按当前语言生成最小调用示例，便于研发快速粘贴接入。
  try {
    await navigator.clipboard.writeText(buildSampleCallCode())
    ElMessage.success('最小调用示例已复制')
  } catch (error) {
    ElMessage.error('复制最小调用示例失败')
  }
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
    const response = await versionReviewApi.getVersionReviews(queryPsuId.value, versionReviewPagination.page, versionReviewPagination.size)
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
  schemaParamRows.value = [createEmptySchemaParamRow()]
  datasetParamRows.value = []
  syncMenuFromRoute()
  loadPsus()
  loadVersionReviews()
})

watch(
  () => route.query.menu,
  () => {
    syncMenuFromRoute()
  }
)
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

:deep(.focused-review-row) {
  --el-table-tr-bg-color: #ecf5ff;
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

.dataset-param-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.dataset-param-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.dataset-param-name {
  font-weight: 600;
  color: #303133;
}

.dataset-param-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.schema-param-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.schema-param-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

h2, h3 {
  margin-bottom: 20px;
}
</style>
