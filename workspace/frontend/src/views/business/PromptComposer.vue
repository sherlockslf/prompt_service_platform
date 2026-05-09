<template>
  <div class="prompt-composer">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button @click="goBack" icon="ArrowLeft">返回</el-button>
          <span class="title">Prompt Composer - {{ psuName || '请选择PSU' }}</span>
          <el-select
            v-model="selectedPsuId"
            filterable
            clearable
            remote
            reserve-keyword
            class="psu-selector"
            placeholder="请选择PSU"
            :remote-method="searchPsuByName"
            :loading="loadingPsuOptions"
            @change="handlePsuChange"
          >
            <el-option
              v-for="psu in availablePsus"
              :key="psu.id"
              :label="psu.name"
              :value="psu.id"
            />
          </el-select>
        </div>
        <div class="header-right">
          <el-tag :type="statusTagType" size="large">{{ statusText }}</el-tag>
          <span v-if="updatedAt" class="update-time">最后更新: {{ formatTime(updatedAt) }}</span>
        </div>
      </el-header>

      <el-container class="main-container">
        <el-aside width="280px" class="left-panel">
          <SchemaVariablePanel
            :schema-fields="schemaFields"
            :search-query="varSearch"
            @insert-variable="insertVariable"
            @update:search-query="varSearch = $event"
          />
          <div class="dataset-panel-left">
            <h4>测试数据集</h4>
            <el-select
              v-model="selectedDatasetId"
              filterable
              clearable
              style="width: 100%"
              placeholder="请选择测试数据集"
              :loading="loadingDatasets"
              @change="handleDatasetSelectionChange"
            >
              <el-option
                v-for="ds in datasets"
                :key="ds.id"
                :label="ds.name"
                :value="ds.id"
              />
            </el-select>
            <div v-if="selectedDatasetId" class="dataset-preview-list">
              <el-card
                v-for="ds in datasets.filter(item => item.id === selectedDatasetId)"
                :key="`preview-${ds.id}`"
                class="dataset-preview-card"
                shadow="never"
              >
                <template #header>
                  <div class="dataset-card-header">
                    <span>{{ ds.name }}</span>
                    <el-button text size="small" @click="toggleDatasetPreview">
                      {{ datasetPreviewExpanded ? '收起测试数据' : '展开测试数据' }}
                    </el-button>
                  </div>
                </template>
                <pre v-if="datasetPreviewExpanded" class="dataset-json">{{ formatJsonText(ds.dataContent) }}</pre>
                <div v-else class="dataset-collapsed-tip">测试数据已收起，点击“展开测试数据”查看全文</div>
              </el-card>
            </div>
          </div>
        </el-aside>

        <el-main class="center-panel">
          <el-empty
            v-if="!selectedPsuId"
            description="请先选择PSU，再进行Prompt编排、渲染预览和接口测试"
          />
          <template v-else>
          <PromptTextEditor
            v-model:content="content"
            :readonly="isReadonly"
            @update:content="onContentChange"
            ref="editorRef"
          />
          <div class="preview-panel">
            <h4>渲染预览</h4>
            <el-button
              @click="runRenderPreview"
              :disabled="isReadonly"
              :loading="rendering"
            >渲染预览</el-button>
            <el-alert
              v-if="missingVars.length > 0"
              style="margin-top: 8px"
              type="error"
              :closable="false"
              :title="`缺失变量: ${missingVars.join(', ')}`"
            />
            <el-input
              v-model="renderedPrompt"
              style="margin-top: 8px"
              type="textarea"
              :rows="6"
              placeholder="渲染后的Prompt会展示在这里，可直接编辑后做模型测试"
            />
          </div>
          <div class="test-result-panel">
            <h4>接口测试（统一后端测试接口）</h4>
            <el-button
              style="margin-top: 8px"
              type="primary"
              :loading="testingModel"
              @click="runModelChatTest"
            >执行接口测试</el-button>
            <el-input
              v-model="modelTestOutput"
              style="margin-top: 8px"
              type="textarea"
              :rows="8"
              readonly
              placeholder="最近一次接口测试结果将显示在这里"
            />
            <el-alert
              v-if="modelTestError"
              style="margin-top: 8px"
              type="error"
              :closable="false"
              :title="modelTestError"
            />
          </div>
          </template>
        </el-main>

        <el-aside width="320px" class="right-panel">
          <InjectionSummaryPanel
            :tokens="tokens"
            :injection-plan="injectionPlan"
            :validation-errors="validationErrors"
            :validation-warnings="validationWarnings"
            :schema-fields="schemaFields"
          />

          <div class="param-set-panel">
            <h4>参数集（覆盖写）</h4>
            <el-input
              v-model="paramSetContent"
              type="textarea"
              :rows="6"
              placeholder="请输入参数集JSON"
            />
            <el-button style="margin-top: 8px" size="small" @click="saveParamSet">保存参数集</el-button>
          </div>

          <div class="action-bar">
            <el-button
              type="primary"
              @click="saveDraft"
              :disabled="isReadonly"
              :loading="saving"
            >保存草稿</el-button>
            <el-button
              @click="runValidate"
              :disabled="isReadonly"
              :loading="validating"
            >校验</el-button>
            <el-button
              type="warning"
              @click="handleSubmit"
              :disabled="isReadonly"
              :loading="submitting"
            >提交审核</el-button>
          </div>

          <div v-if="selectedPsuId" class="dataset-runner-panel">
            <h4>测试集批量运行</h4>
            <TestDatasetRunner :psu-id="Number(selectedPsuId)" />
          </div>
        </el-aside>
      </el-container>
    </el-container>

    <el-dialog v-model="showLeaveDialog" title="未保存提示" width="400px">
      <p>您有未保存的更改，确定要离开吗？</p>
      <template #footer>
        <el-button @click="showLeaveDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmLeave">确定离开</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { compositionApi, schemaApi, psuApi, testDatasetApi, promptApi, paramSetApi } from '@/services/api'
import SchemaVariablePanel from '@/components/composer/SchemaVariablePanel.vue'
import PromptTextEditor from '@/components/composer/PromptTextEditor.vue'
import InjectionSummaryPanel from '@/components/composer/InjectionSummaryPanel.vue'
import TestDatasetRunner from '@/components/composer/TestDatasetRunner.vue'

const route = useRoute()
const router = useRouter()
const psuId = computed(() => {
  const id = Number(route.query.psuId)
  return Number.isInteger(id) && id > 0 ? id : null
})
const selectedPsuId = ref(psuId.value)
const availablePsus = ref([])
const loadingPsuOptions = ref(false)
let searchPsuDebounceTimer = null

const content = ref('')
const compositionId = ref(null)
const tokens = ref([])
const injectionPlan = ref([])
const schemaFields = ref([])
const psuName = ref('')
const status = ref('DRAFT')
const updatedAt = ref('')
const saving = ref(false)
const validating = ref(false)
const rendering = ref(false)
const submitting = ref(false)
const testingModel = ref(false)
const varSearch = ref('')
const showLeaveDialog = ref(false)
const hasUnsavedChanges = ref(false)
const validationErrors = ref([])
const validationWarnings = ref([])
const renderedPrompt = ref('')
const missingVars = ref([])
const datasets = ref([])
const loadingDatasets = ref(false)
const selectedDatasetId = ref(null)
const selectedDatasetInput = ref({})
const paramSetInput = ref({})
const paramSetContent = ref('{}')
const datasetPreviewExpanded = ref(false)
const modelTestOutput = ref('')
const modelTestError = ref('')
const editorRef = ref(null)

// 已取消按版本状态锁定编辑，仅归档态不可编辑
const isReadonly = computed(() => status.value === 'ARCHIVED')

const statusTagType = computed(() => {
  const map = { DRAFT: 'success', CANDIDATE: 'warning', FORMAL: 'primary', ARCHIVED: 'info' }
  return map[status.value] || 'info'
})

const statusText = computed(() => {
  const map = { DRAFT: '草稿', CANDIDATE: '发布候选', FORMAL: '正式版本', ARCHIVED: '归档' }
  return map[status.value] || status.value
})

onMounted(async () => {
  // 页面初始化先加载PSU列表，选定PSU后再加载编排上下文
  await loadAvailablePsus()
  await initializeBySelectedPsu()
})

onBeforeUnmount(() => {
  if (searchPsuDebounceTimer) {
    clearTimeout(searchPsuDebounceTimer)
  }
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

watch(content, () => {
  parseTokens()
  hasUnsavedChanges.value = true
})

watch(psuId, async (newId) => {
  if (newId && newId !== selectedPsuId.value) {
    selectedPsuId.value = newId
    await initializeBySelectedPsu()
  }
})

watch(selectedPsuId, async (newId, oldId) => {
  if (newId === oldId) return
  if (!newId) {
    resetComposerState()
    if (route.query.psuId) {
      router.replace('/business/composer')
    }
    return
  }
  if (Number(route.query.psuId) !== Number(newId)) {
    router.replace({ path: '/business/psus/composer', query: { psuId: String(newId) } })
  }
  await initializeBySelectedPsu()
})

const handleBeforeUnload = (e) => {
  if (hasUnsavedChanges.value && !isReadonly.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

window.addEventListener('beforeunload', handleBeforeUnload)

async function loadPsuName() {
  if (!selectedPsuId.value) {
    psuName.value = ''
    return
  }
  try {
    const res = await psuApi.getPsus(1, 100)
    const psu = res.data.content.find(p => p.id === selectedPsuId.value)
    if (psu) psuName.value = psu.name
  } catch (e) {
    console.error('Failed to load PSU name', e)
  }
}

async function loadAvailablePsus() {
  loadingPsuOptions.value = true
  try {
    // 默认仅加载10条，作为下拉初始候选
    const res = await psuApi.getPsus(1, 10)
    availablePsus.value = Array.isArray(res.data?.content) ? res.data.content : []
    if (!selectedPsuId.value && availablePsus.value.length === 1) {
      selectedPsuId.value = availablePsus.value[0].id
    }
  } catch (e) {
    console.error('Failed to load PSU list', e)
    availablePsus.value = []
  } finally {
    loadingPsuOptions.value = false
  }
}

function searchPsuByName(keyword) {
  if (searchPsuDebounceTimer) {
    clearTimeout(searchPsuDebounceTimer)
  }
  searchPsuDebounceTimer = setTimeout(async () => {
    loadingPsuOptions.value = true
    try {
      const res = await psuApi.getPsus(1, 10, keyword || '')
      availablePsus.value = Array.isArray(res.data?.content) ? res.data.content : []
    } catch (e) {
      console.error('PSU name search failed', e)
      availablePsus.value = []
    } finally {
      loadingPsuOptions.value = false
    }
  }, 250)
}

async function initializeBySelectedPsu() {
  if (!selectedPsuId.value) {
    resetComposerState()
    return
  }
  await loadPsuName()
  await loadComposition()
  await loadSchema()
  await loadParamSet()
  await loadTestDatasets()
}

function resetComposerState() {
  content.value = ''
  compositionId.value = null
  tokens.value = []
  injectionPlan.value = []
  schemaFields.value = []
  psuName.value = ''
  status.value = 'DRAFT'
  updatedAt.value = ''
  validationErrors.value = []
  validationWarnings.value = []
  renderedPrompt.value = ''
  missingVars.value = []
  datasets.value = []
  selectedDatasetId.value = null
  selectedDatasetInput.value = {}
  paramSetInput.value = {}
  paramSetContent.value = '{}'
  datasetPreviewExpanded.value = false
  modelTestOutput.value = ''
  modelTestError.value = ''
  hasUnsavedChanges.value = false
}

function handlePsuChange() {
  // 由selectedPsuId侦听器统一处理URL切换与数据加载
}

async function loadComposition() {
  if (!selectedPsuId.value) return
  try {
    const res = await compositionApi.getComposition(selectedPsuId.value)
    if (res.data && res.data.content !== undefined) {
      compositionId.value = res.data.id
      content.value = res.data.content || ''
      status.value = res.data.status
      updatedAt.value = res.data.updatedAt
      if (res.data.specJson) {
        try {
          const spec = typeof res.data.specJson === 'string' ? JSON.parse(res.data.specJson) : res.data.specJson
          if (spec.tokens) tokens.value = spec.tokens
          if (spec.injectionPlan) injectionPlan.value = spec.injectionPlan
        } catch (e) {
          console.error('Failed to parse specJson', e)
        }
      }
    }
  } catch (e) {
    if (e.response && e.response.status !== 404) {
      ElMessage.error('加载编排草稿失败')
    }
  }
  parseTokens()
}

async function loadSchema() {
  if (!selectedPsuId.value) return
  try {
    const res = await schemaApi.getSchema(selectedPsuId.value)
    let schemaContent = res.data.schemaContent || '{}'
    if (typeof schemaContent === 'string') {
      schemaContent = JSON.parse(schemaContent)
    }
    schemaFields.value = parseSchemaFields(schemaContent)
  } catch (e) {
    console.error('加载Schema失败:', e)
    schemaFields.value = []
  }
}

async function loadTestDatasets() {
  if (!selectedPsuId.value) return
  // 加载当前PSU的测试数据集供多选调试
  loadingDatasets.value = true
  try {
    const res = await testDatasetApi.getTestDatasets(selectedPsuId.value)
    datasets.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error('加载测试数据集失败:', e)
    datasets.value = []
  } finally {
    loadingDatasets.value = false
  }
}

async function loadParamSet() {
  if (!selectedPsuId.value) return
  try {
    const res = await paramSetApi.getParamSet(selectedPsuId.value)
    const raw = res.data?.paramSetContent || '{}'
    paramSetContent.value = formatJsonText(raw)
    paramSetInput.value = parseJsonSafe(raw)
  } catch (e) {
    // 参数集允许首次为空，不阻断页面
    paramSetContent.value = '{}'
    paramSetInput.value = {}
  }
}

async function saveParamSet() {
  if (!selectedPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  try {
    const parsed = parseJsonSafe(paramSetContent.value)
    await paramSetApi.updateParamSet(selectedPsuId.value, {
      paramSetContent: JSON.stringify(parsed),
      changeLog: 'business-composer-overwrite'
    })
    paramSetInput.value = parsed
    ElMessage.success('参数集已保存')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '参数集保存失败，请检查JSON格式')
  }
}

function parseSchemaFields(schema) {
  const fields = []

  const walk = (node, parentPath = '', requiredSet = new Set()) => {
    if (!node || typeof node !== 'object') return
    if (node.properties && typeof node.properties === 'object') {
      const localRequired = new Set(node.required || [])
      for (const [name, prop] of Object.entries(node.properties)) {
        const currentPath = parentPath ? `${parentPath}.${name}` : name
        fields.push({
          name: currentPath,
          type: prop?.type || 'string',
          required: localRequired.has(name) || requiredSet.has(currentPath),
          description: prop?.description || ''
        })
        if (prop?.type === 'object' || prop?.properties) {
          walk(prop, currentPath)
        } else if (prop?.type === 'array' && prop?.items) {
          const itemPath = `${currentPath}[*]`
          fields.push({
            name: itemPath,
            type: 'array',
            required: localRequired.has(name),
            description: prop?.description || ''
          })
          if (prop.items?.properties || prop.items?.type === 'object') {
            walk(prop.items, itemPath)
          }
        }
      }
      return
    }

    for (const [name, value] of Object.entries(node)) {
      const currentPath = parentPath ? `${parentPath}.${name}` : name
      fields.push({
        name: currentPath,
        type: inferType(value),
        required: false,
        description: ''
      })
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        walk(value, currentPath)
      }
    }
  }

  walk(schema)
  return fields
}

function inferType(value) {
  if (value === null || value === undefined) return 'null'
  if (Array.isArray(value)) return 'array'
  const type = typeof value
  if (type === 'number') return 'number'
  if (type === 'boolean') return 'boolean'
  if (type === 'object') return 'object'
  return 'string'
}

function parseTokens() {
  if (!content.value) {
    tokens.value = []
    injectionPlan.value = []
    return
  }

  const VAR_PATTERN = /\{\{\s*([^}]+?)\s*\}\}/g
  const newTokens = []
  const planMap = new Map()
  let lastIndex = 0
  let match

  while ((match = VAR_PATTERN.exec(content.value)) !== null) {
    if (match.index > lastIndex) {
      newTokens.push({ type: 'TEXT', value: content.value.substring(lastIndex, match.index) })
    }
    const path = match[1].trim()
    newTokens.push({ type: 'VAR', path, source: 'INPUT_SCHEMA' })
    if (!planMap.has(path)) {
      planMap.set(path, { path, required: isFieldRequired(path), count: 0 })
    }
    planMap.get(path).count++
    lastIndex = match.index + match[0].length
  }

  if (lastIndex < content.value.length) {
    newTokens.push({ type: 'TEXT', value: content.value.substring(lastIndex) })
  }

  tokens.value = newTokens
  injectionPlan.value = Array.from(planMap.values())
}

function isFieldRequired(path) {
  const field = schemaFields.value.find(f => f.name === path)
  return field ? field.required : false
}

function insertVariable(field) {
  if (isReadonly.value) return
  if (editorRef.value && typeof editorRef.value.insertAtCursor === 'function') {
    editorRef.value.insertAtCursor(`{{${field.name}}}`)
  } else {
    content.value += `{{${field.name}}}`
  }
}

function onContentChange(newContent) {
  content.value = newContent
}

async function saveDraft() {
  if (isReadonly.value) return
  if (!selectedPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  saving.value = true
  try {
    const spec = {
      content: content.value,
      tokens: tokens.value,
      injectionPlan: injectionPlan.value,
      assembledFragments: []
    }
    await compositionApi.saveDraft(selectedPsuId.value, spec)
    ElMessage.success('草稿保存成功')
    hasUnsavedChanges.value = false
    const res = await compositionApi.getComposition(selectedPsuId.value)
    if (res.data) updatedAt.value = res.data.updatedAt
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.error || e.message))
  } finally {
    saving.value = false
  }
}

async function runValidate() {
  if (!selectedPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  validating.value = true
  validationErrors.value = []
  validationWarnings.value = []

  if (hasUnclosedBraces(content.value)) {
    validationErrors.value.push({ code: 'UNCLOSED_BRACES', message: '存在未闭合的 {{ 占位符' })
  }

  const emptyVars = content.value.match(/\{\{\s*\}\}/g)
  if (emptyVars) {
    validationErrors.value.push({ code: 'EMPTY_VAR', message: '存在空变量: {{ }}' })
  }

  try {
    const spec = {
      content: content.value,
      tokens: tokens.value,
      injectionPlan: injectionPlan.value,
      assembledFragments: []
    }
    const res = await compositionApi.validate(selectedPsuId.value, spec)
    if (res.data.errors) validationErrors.value.push(...res.data.errors)
    if (res.data.warnings) validationWarnings.value.push(...res.data.warnings)

    if (res.data.ok) {
      ElMessage.success('校验通过')
    } else {
      ElMessage.warning(`校验失败: ${res.data.errors.length} 个错误`)
    }
  } catch (e) {
    ElMessage.error('校验请求失败')
  } finally {
    validating.value = false
  }
}

async function runRenderPreview() {
  rendering.value = true
  missingVars.value = []
  renderedPrompt.value = ''
  try {
    // 渲染优先使用参数集，测试数据集作为覆盖输入。
    let input = cloneObject(paramSetInput.value)
    if (Object.keys(selectedDatasetInput.value || {}).length > 0) {
      input = { ...input, ...cloneObject(selectedDatasetInput.value) }
    }
    // 预览始终基于当前页面编辑中的文本，不依赖数据库中的草稿内容
    const preview = renderFromCurrentContent(content.value, input)
    renderedPrompt.value = preview.renderedPrompt
    missingVars.value = preview.missingVars
  } catch (e) {
    ElMessage.error('渲染预览失败')
  } finally {
    rendering.value = false
  }
}

function getValueByPath(source, rawPath) {
  // 支持 a.b.c 与数组下标路径读取（如 items[0].name）
  if (!source || !rawPath) return undefined
  const normalizedPath = String(rawPath).replace(/\[\*\]/g, '.0')
  const parts = normalizedPath
    .replace(/\[(\d+)\]/g, '.$1')
    .split('.')
    .filter(Boolean)
  let current = source
  for (const part of parts) {
    if (current === null || current === undefined) return undefined
    current = current[part]
  }
  return current
}

function renderFromCurrentContent(templateContent, input) {
  // 基于当前编辑内容执行变量替换，并返回缺失变量列表
  const missingSet = new Set()
  const renderedPrompt = (templateContent || '').replace(/\{\{\s*([^}]+?)\s*\}\}/g, (_, rawPath) => {
    const path = String(rawPath).trim()
    const value = getValueByPath(input, path)
    if (value === undefined || value === null) {
      missingSet.add(path)
      return ''
    }
    if (typeof value === 'object') {
      return JSON.stringify(value)
    }
    return String(value)
  })
  return {
    renderedPrompt,
    missingVars: Array.from(missingSet)
  }
}

function parseDatasetInput(dataset) {
  // 解析测试数据JSON并统一返回对象
  if (!dataset || !dataset.dataContent) return {}
  try {
    return typeof dataset.dataContent === 'string' ? JSON.parse(dataset.dataContent) : dataset.dataContent
  } catch {
    throw new Error(`测试数据集 ${dataset.name} JSON格式错误`)
  }
}

function applyDatasetToPreview() {
  // 将选中的测试数据集载入内部入参，不在中间预览区重复展示全文。
  if (!selectedDatasetId.value) {
    selectedDatasetInput.value = {}
    return
  }
  const dataset = datasets.value.find(ds => ds.id === selectedDatasetId.value)
  if (!dataset) {
    selectedDatasetInput.value = {}
    return
  }
  try {
    selectedDatasetInput.value = parseDatasetInput(dataset)
    datasetPreviewExpanded.value = false
  } catch (e) {
    selectedDatasetInput.value = {}
    ElMessage.error(e.message)
  }
}

async function handleDatasetSelectionChange() {
  // 选择测试集后自动载入数据并触发当前页面提示词渲染预览
  if (!selectedDatasetId.value) {
    selectedDatasetInput.value = {}
    renderedPrompt.value = ''
    missingVars.value = []
    return
  }
  applyDatasetToPreview()
  await runRenderPreview()
}

function toggleDatasetPreview() {
  // 左侧测试数据全文支持展开/收起，减少长文本干扰。
  datasetPreviewExpanded.value = !datasetPreviewExpanded.value
}

function cloneObject(value) {
  // 通过JSON序列化做安全拷贝，避免渲染阶段污染源对象。
  try {
    return JSON.parse(JSON.stringify(value || {}))
  } catch {
    return {}
  }
}

async function runModelChatTest() {
  await runModelChatTestWithRetry(false)
}

async function runModelChatTestWithRetry(fromRetry) {
  // 统一走后端Prompt测试接口，避免前端出现多套测试路径
  if (!selectedPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }
  testingModel.value = true
  modelTestError.value = ''
  modelTestOutput.value = ''
  let tipTimer10 = null
  let tipTimer20 = null
  let abortTimer30 = null
  let forceAbortedByTimeout = false
  const controller = new AbortController()
  try {
    if (fromRetry) {
      ElMessage.info('已重试请求，请稍候')
    }
    tipTimer10 = setTimeout(() => {
      ElMessage.info('大模型响应中，请稍候...')
    }, 10000)
    tipTimer20 = setTimeout(() => {
      ElMessage.warning('请求排队中，请继续等待...')
    }, 20000)
    abortTimer30 = setTimeout(() => {
      forceAbortedByTimeout = true
      controller.abort()
    }, 30000)

    // 测试输入优先使用参数集，并用测试数据集做覆盖
    let input = cloneObject(paramSetInput.value)
    if (Object.keys(selectedDatasetInput.value || {}).length > 0) {
      input = { ...input, ...cloneObject(selectedDatasetInput.value) }
    }
    // 使用当前页面“渲染预览”里的文本作为最终模型输入，保证结果与预览一致。
    input.renderedPrompt = renderedPrompt.value || ''
    const response = await promptApi.testPrompt(selectedPsuId.value, input, {
      timeout: 30000,
      signal: controller.signal
    })
    const data = response.data || {}
    modelTestOutput.value = data.modelOutput || ''
    if (Array.isArray(data.missingVars) && data.missingVars.length > 0) {
      modelTestError.value = `缺失变量: ${data.missingVars.join(', ')}`
    }
    ElMessage.success('接口测试完成')
  } catch (e) {
    if (forceAbortedByTimeout || e.code === 'ECONNABORTED' || e.code === 'ERR_CANCELED') {
      modelTestError.value = '请求超时（30秒），本次测试已终止'
      ElMessage.error(modelTestError.value)
      try {
        await ElMessageBox.confirm('请求已超时并终止，是否重试？', '请求超时', {
          confirmButtonText: '重试',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await runModelChatTestWithRetry(true)
      } catch {
        // 用户取消重试，不做额外处理
      }
    } else {
      modelTestError.value = e.response?.data?.message || e.message || '接口测试失败'
      ElMessage.error(modelTestError.value)
    }
  } finally {
    if (tipTimer10) clearTimeout(tipTimer10)
    if (tipTimer20) clearTimeout(tipTimer20)
    if (abortTimer30) clearTimeout(abortTimer30)
    testingModel.value = false
  }
}

function formatJsonText(data) {
  // 格式化JSON文本用于页面预览
  if (!data) return '{}'
  try {
    const parsed = typeof data === 'string' ? JSON.parse(data) : data
    return JSON.stringify(parsed, null, 2)
  } catch {
    return String(data)
  }
}

function parseJsonSafe(data) {
  if (!data) return {}
  return typeof data === 'string' ? JSON.parse(data) : data
}

function hasUnclosedBraces(text) {
  let i = 0
  while (i < text.length) {
    if (i + 1 < text.length && text[i] === '{' && text[i + 1] === '{') {
      const closeIdx = text.indexOf('}}', i + 2)
      if (closeIdx === -1) return true
      i = closeIdx + 2
    } else {
      i++
    }
  }
  return false
}

async function handleSubmit() {
  if (isReadonly.value) return
  if (!selectedPsuId.value) {
    ElMessage.warning('请先选择PSU')
    return
  }

  try {
    await ElMessageBox.confirm('确认提交当前编排进行审核？', '确认提交', {
      confirmButtonText: '确认提交',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    await runValidate()
    if (validationErrors.value.length > 0) {
      ElMessage.error('请先修复校验错误后再提交')
      return
    }
    await compositionApi.submit(selectedPsuId.value)
    ElMessage.success('提交成功')
    status.value = 'CANDIDATE'
    hasUnsavedChanges.value = false
    const res = await compositionApi.getComposition(selectedPsuId.value)
    if (res.data) {
      compositionId.value = res.data.id
      status.value = res.data.status
      updatedAt.value = res.data.updatedAt
    }
  } catch (e) {
    ElMessage.error('提交失败: ' + (e.response?.data?.error || e.message))
  } finally {
    submitting.value = false
  }
}

function goBack() {
  if (hasUnsavedChanges.value && !isReadonly.value) {
    showLeaveDialog.value = true
  } else {
    router.push('/business')
  }
}

function confirmLeave() {
  showLeaveDialog.value = false
  hasUnsavedChanges.value = false
  router.push('/business')
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.prompt-composer {
  height: 100vh;
  display: flex;
  flex-direction: column;
  color: var(--neo-text);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--neo-border);
  background: linear-gradient(120deg, rgba(15, 35, 58, 0.92), rgba(12, 23, 41, 0.86));
  padding: 0 20px;
  backdrop-filter: blur(8px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.psu-selector {
  width: 240px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.update-time {
  font-size: 12px;
  color: var(--neo-text-dim);
}

.main-container {
  flex: 1;
  overflow: hidden;
}

.left-panel {
  background: rgba(10, 23, 41, 0.76);
  border-right: 1px solid var(--neo-border);
  overflow-y: auto;
  padding: 16px;
}

.dataset-panel-left {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #dcdfe6;
}

.dataset-panel-left h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.dataset-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.dataset-collapsed-tip {
  padding: 8px;
  font-size: 12px;
  color: var(--neo-text-dim);
  background: rgba(6, 16, 30, 0.74);
  border-radius: 4px;
}

.center-panel {
  padding: 16px;
  overflow-y: auto;
}

.right-panel {
  background: rgba(10, 23, 41, 0.76);
  border-left: 1px solid var(--neo-border);
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.action-bar {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--neo-border);
}

.action-bar :deep(.el-button) {
  width: 100%;
  min-height: 48px;
  margin: 0 !important;
  border-radius: 10px;
  font-size: 20px;
  font-weight: 700;
}

.param-set-panel {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed rgba(95, 158, 199, 0.35);
}

.param-set-panel h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.preview-panel {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed rgba(95, 158, 199, 0.35);
}

.preview-panel h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.test-result-panel {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed rgba(95, 158, 199, 0.35);
}

.test-result-panel h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.chat-control-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-history {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-item {
  border: 1px solid var(--neo-border);
  border-radius: 6px;
  padding: 10px;
}

.chat-item.user {
  background: rgba(6, 16, 30, 0.72);
}

.chat-item.assistant {
  background: rgba(13, 36, 56, 0.82);
  border-color: var(--neo-border-strong);
}

.chat-role {
  font-size: 12px;
  color: var(--neo-text-dim);
  margin-bottom: 6px;
}

.chat-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  color: var(--neo-text);
}

.dataset-preview-list,
.batch-result-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dataset-preview-card,
.batch-result-card {
  border: 1px solid #e4e7ed;
}

.dataset-json {
  margin: 0;
  padding: 8px;
  border-radius: 4px;
  background: rgba(6, 16, 30, 0.74);
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  max-height: 220px;
  overflow-y: auto;
  color: #c7f4ff;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper) {
  background: rgba(6, 16, 30, 0.88) !important;
  box-shadow: inset 0 0 0 1px var(--neo-border) !important;
  color: var(--neo-text) !important;
}

:deep(.el-button--primary) {
  border: none !important;
  background: linear-gradient(110deg, var(--neo-accent), var(--neo-accent-2)) !important;
  color: #071120 !important;
  font-weight: 700;
}

:deep(.el-button--warning) {
  border: none !important;
  background: linear-gradient(110deg, #ffd35b, #ff9f4d) !important;
  color: #1d1002 !important;
  font-weight: 700;
}
</style>
