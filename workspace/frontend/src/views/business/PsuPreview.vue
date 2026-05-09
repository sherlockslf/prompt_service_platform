<template>
  <div class="psu-preview-page">
    <div class="header">
      <el-button @click="goBack">返回列表</el-button>
      <h2>PSU 预览</h2>
    </div>

    <el-card class="neo-card" v-loading="loading">
      <template #header>
        <div class="card-title">
          <span>{{ psuInfo.psuId || '-' }}</span>
          <el-tag :type="psuInfo.status === 'FORMAL' ? 'success' : 'info'">{{ psuInfo.status || '-' }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称">{{ psuInfo.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标签">{{ psuInfo.tag || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ psuInfo.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="block">
        <div class="block-header">
          <h3>Prompt</h3>
          <el-button type="primary" link @click="goPromptEdit">编辑提示词</el-button>
        </div>
        <el-input :model-value="promptContent" type="textarea" :rows="10" readonly />
      </div>

      <div class="block">
        <div class="block-header">
          <h3>JSON Schema</h3>
          <el-button type="primary" link @click="goSchemaEdit">编辑对话参数</el-button>
        </div>
        <el-input :model-value="schemaContent" type="textarea" :rows="12" readonly />
      </div>

      <div class="block">
        <div class="block-header">
          <h3>测试数据</h3>
        </div>
        <el-table class="table-neo" :data="datasets" empty-text="暂无测试数据">
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="name" label="名称" width="220" />
          <el-table-column label="数据内容">
            <template #default="{ row }">
              <pre class="json-preview">{{ formatJson(row.dataContent) }}</pre>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { psuApi, schemaApi, testDatasetApi, compositionApi } from '@/services/api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const psuInfo = ref({})
const promptContent = ref('')
const schemaContent = ref('{}')
const datasets = ref([])
const psuId = computed(() => Number(route.query.psuId))

const loadData = async () => {
  if (!Number.isInteger(psuId.value) || psuId.value <= 0) {
    ElMessage.error('无效的PSU ID')
    router.push('/business')
    return
  }
  loading.value = true
  try {
    const [psuRes, compositionRes, schemaRes, datasetRes] = await Promise.allSettled([
      psuApi.getPsuById(psuId.value),
      compositionApi.getComposition(psuId.value),
      schemaApi.getSchema(psuId.value),
      testDatasetApi.getTestDatasets(psuId.value)
    ])

    if (psuRes.status !== 'fulfilled') {
      throw psuRes.reason
    }
    psuInfo.value = psuRes.value.data || {}

    // 规则统一：预览仅展示编排草稿内容；未保存草稿时保持空字符串。
    if (compositionRes.status === 'fulfilled') {
      promptContent.value = compositionRes.value?.data?.content || ''
    } else {
      promptContent.value = ''
    }

    schemaContent.value = schemaRes.status === 'fulfilled'
      ? formatJson(schemaRes.value.data?.schemaContent || '{}')
      : '{}'
    datasets.value = datasetRes.status === 'fulfilled'
      ? (Array.isArray(datasetRes.value.data) ? datasetRes.value.data : [])
      : []
  } catch (error) {
    console.error('加载PSU预览失败:', error)
    ElMessage.error('加载PSU预览失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/business')
}

const goPromptEdit = () => {
  router.push({ path: '/business/psus/composer', query: { psuId: String(psuId.value) } })
}

const goSchemaEdit = () => {
  router.push(`/business?menu=5&psuId=${psuId.value}`)
}

const formatJson = (raw) => {
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return JSON.stringify(parsed, null, 2)
  } catch {
    return typeof raw === 'string' ? raw : JSON.stringify(raw, null, 2)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.psu-preview-page {
  padding: 22px;
  color: var(--neo-text);
}

.header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.header h2 {
  margin: 0;
  letter-spacing: 0.02em;
}

.neo-card {
  border-radius: var(--neo-radius-lg);
  border: 1px solid var(--neo-border);
  background: var(--neo-surface);
  box-shadow: var(--neo-shadow);
  backdrop-filter: blur(8px);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--neo-text);
}

.block {
  margin-top: 20px;
  border-top: 1px solid rgba(98, 170, 214, 0.18);
  padding-top: 14px;
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.json-preview {
  margin: 0;
  max-height: 160px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  padding: 10px;
  border-radius: 10px;
  background: rgba(6, 16, 30, 0.74);
  color: #c7f4ff;
}

:deep(.el-button--primary.is-link) {
  color: #85ffe7 !important;
  font-weight: 600;
}

:deep(.el-descriptions) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-descriptions__body),
:deep(.el-descriptions__table),
:deep(.el-descriptions-item__cell),
:deep(.el-descriptions__label.el-descriptions__cell),
:deep(.el-descriptions__content.el-descriptions__cell) {
  background: rgba(10, 23, 41, 0.7) !important;
  color: var(--neo-text) !important;
  border-color: rgba(95, 158, 199, 0.25) !important;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  background: rgba(6, 16, 30, 0.88) !important;
  box-shadow: inset 0 0 0 1px var(--neo-border) !important;
  color: var(--neo-text) !important;
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

@media (max-width: 768px) {
  .psu-preview-page {
    padding: 12px;
  }
}
</style>
