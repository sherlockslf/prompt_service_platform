<template>
  <div class="test-dataset-runner">
    <el-form label-width="100px">
      <el-form-item label="选择测试集">
        <el-select
          v-model="selectedDatasetIds"
          multiple
          collapse-tags
          collapse-tags-tooltip
          filterable
          clearable
          placeholder="请选择一个或多个测试数据集"
          style="width: 100%"
          @change="onDatasetChange"
        >
          <el-option
            v-for="ds in datasets"
            :key="ds.id"
            :label="ds.name"
            :value="ds.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          @click="runTest"
          :loading="running"
          :disabled="selectedDatasetIds.length === 0"
        >批量运行测试</el-button>
      </el-form-item>
    </el-form>

    <div v-if="testResult" class="test-result">
      <el-divider>测试结果</el-divider>
      <div class="result-summary">
        <el-tag type="info">总计: {{ testResult.totalCases }}</el-tag>
        <el-tag type="success" style="margin-left: 8px">成功: {{ testResult.successCases }}</el-tag>
        <el-tag type="danger" style="margin-left: 8px">失败: {{ testResult.failedCases }}</el-tag>
        <el-tag style="margin-left: 8px">状态: {{ testResult.status || '-' }}</el-tag>
      </div>

        <el-table :data="testResult.items" style="width: 100%; margin-top: 12px" size="small">
          <el-table-column prop="name" label="用例名称" width="150" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="80" align="center">
          <template #default="{ row }">
            {{ row.latencyMs }}ms
          </template>
        </el-table-column>
        <el-table-column label="错误摘要" min-width="150">
          <template #default="{ row }">
            <span v-if="row.exceptionReason || row.error" class="error-text">{{ row.exceptionReason || row.error }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="history-panel">
      <div class="history-header">
        <span class="history-title">运行历史</span>
        <span class="history-meta">最近50条</span>
      </div>
      <el-table :data="runHistory" size="small" style="width: 100%">
        <el-table-column prop="runId" label="Run ID" width="90" />
        <el-table-column prop="datasetId" label="数据集ID" width="100" />
        <el-table-column label="统计" min-width="140">
          <template #default="{ row }">
            {{ row.successCases }}/{{ row.totalCases }} 成功
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column label="异常原因" min-width="160">
          <template #default="{ row }">
            <span v-if="row.exceptionReason" class="error-text">{{ row.exceptionReason }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openRunDetail(row.runId)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showDetailDialog" title="测试详情" width="700px">
      <div v-if="detailItem" class="detail-content">
        <h5>输入参数</h5>
        <pre class="json-block">{{ formatJson(detailItem.input) }}</pre>
        <h5>渲染后的 Prompt</h5>
        <pre class="prompt-block">{{ detailItem.renderedPrompt || '-' }}</pre>
        <h5>模型输出</h5>
        <pre class="output-block">{{ detailItem.actualOutput || detailItem.modelOutput || '-' }}</pre>
        <div v-if="detailItem.exceptionReason || detailItem.error" class="error-block">
          <strong>错误信息:</strong> {{ detailItem.exceptionReason || detailItem.error }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { testDatasetApi, testRunApi } from '@/services/api'

const props = defineProps({
  psuId: { type: Number, required: true }
})

const datasets = ref([])
const selectedDatasetIds = ref([])
const running = ref(false)
const testResult = ref(null)
const runHistory = ref([])
const showDetailDialog = ref(false)
const detailItem = ref(null)

onMounted(async () => {
  await loadDatasets()
  await loadRunHistory()
})

watch(
  () => props.psuId,
  async () => {
    // PSU切换后重新加载测试集与运行历史，避免展示旧数据
    selectedDatasetIds.value = []
    testResult.value = null
    runHistory.value = []
    await loadDatasets()
    await loadRunHistory()
  }
)

async function loadRunHistory() {
  try {
    const res = await testRunApi.getTestRuns(props.psuId)
    runHistory.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    runHistory.value = []
  }
}

async function loadDatasets() {
  try {
    const res = await testDatasetApi.getTestDatasets(props.psuId)
    datasets.value = res.data
  } catch (e) {
    ElMessage.error('加载测试集失败')
  }
}

function onDatasetChange() {
  testResult.value = null
}

async function runTest() {
  // 对多选测试集逐个执行测试并汇总结果
  if (selectedDatasetIds.value.length === 0) {
    ElMessage.warning('请至少选择一个测试集')
    return
  }

  running.value = true
  testResult.value = null
  try {
    const selected = datasets.value.filter(ds => selectedDatasetIds.value.includes(ds.id))
    const merged = {
      totalCases: 0,
      successCases: 0,
      failedCases: 0,
      status: 'RUNNING',
      exceptionReason: null,
      items: []
    }

    for (const dataset of selected) {
      const res = await testRunApi.runTest(props.psuId, dataset.id, {})
      const data = res.data || {}
      merged.totalCases += Number(data.totalCases || 0)
      merged.successCases += Number(data.successCases || 0)
      merged.failedCases += Number(data.failedCases || 0)
      if (data.exceptionReason && !merged.exceptionReason) {
        // 保留首个异常原因，便于在汇总层快速定位问题。
        merged.exceptionReason = data.exceptionReason
      }
      const items = Array.isArray(data.items) ? data.items : []
      merged.items.push(...items.map(item => ({
        ...item,
        name: `[${dataset.name}] ${item.name || '未命名用例'}`
      })))
    }

    testResult.value = merged
    merged.status = resolveMergedStatus(merged.totalCases, merged.failedCases)
    await loadRunHistory()
    if (merged.failedCases === 0) {
      ElMessage.success('批量测试全部通过')
    } else {
      ElMessage.warning(`批量测试完成: ${merged.failedCases} 个失败`)
    }
  } catch (e) {
    ElMessage.error('运行测试失败: ' + (e.response?.data?.error || e.message))
  } finally {
    running.value = false
  }
}

function showDetail(item) {
  detailItem.value = item
  showDetailDialog.value = true
}

async function openRunDetail(runId) {
  try {
    const response = await testRunApi.getTestRun(runId)
    const data = response.data || {}
    detailItem.value = {
      input: { runId, totalCases: data.totalCases, successCases: data.successCases, failedCases: data.failedCases },
      renderedPrompt: '',
      actualOutput: '',
      error: '',
      exceptionReason: data.exceptionReason || '',
      ...((Array.isArray(data.items) && data.items.length > 0) ? data.items[0] : {})
    }
    showDetailDialog.value = true
  } catch (e) {
    ElMessage.error('加载运行详情失败')
  }
}

function formatJson(obj) {
  if (!obj) return '{}'
  try {
    return typeof obj === 'string' ? JSON.stringify(JSON.parse(obj), null, 2) : JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}

function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  if (Number.isNaN(date.getTime())) return String(dateTime)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function resolveMergedStatus(totalCases, failedCases) {
  if (!totalCases || totalCases <= 0) return 'SUCCESS'
  if (!failedCases || failedCases <= 0) return 'SUCCESS'
  if (failedCases >= totalCases) return 'FAILED'
  return 'PARTIAL_SUCCESS'
}
</script>

<style scoped>
.test-dataset-runner {
  padding: 8px 0;
  color: var(--neo-text);
}

.history-panel {
  margin-top: 8px;
}

.history-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 8px 0 10px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(95, 158, 199, 0.25);
}

.history-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--neo-text);
  line-height: 1;
}

.history-meta {
  font-size: 12px;
  color: var(--neo-text-dim);
  line-height: 1;
}

.result-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.error-text {
  color: #ff8ea4;
  font-size: 12px;
}

.detail-content h5 {
  margin: 12px 0 6px 0;
  font-size: 13px;
  color: var(--neo-text);
}

.json-block, .prompt-block, .output-block {
  background: rgba(6, 16, 30, 0.82);
  border: 1px solid rgba(95, 158, 199, 0.25);
  padding: 12px;
  border-radius: 10px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
  color: #d9f2ff;
}

.error-block {
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(138, 48, 73, 0.2);
  border: 1px solid rgba(255, 142, 164, 0.35);
  border-radius: 10px;
  color: #ffc8d3;
  font-size: 12px;
}

:deep(.el-divider__text) {
  background: transparent !important;
  color: var(--neo-text) !important;
}

:deep(.el-divider--horizontal) {
  border-top-color: rgba(95, 158, 199, 0.25) !important;
}
</style>
