<template>
  <div class="evaluation-center">
    <div class="header">
      <h2>评估中心</h2>
      <div class="actions">
        <el-button type="primary" @click="createTask">创建评估任务</el-button>
        <el-button type="success" :disabled="!currentTaskId" @click="runTask">执行任务</el-button>
      </div>
    </div>

    <el-form :model="form" inline>
      <el-form-item label="PSU">
        <el-select v-model="form.psuId" placeholder="请选择PSU" style="width: 220px" @change="loadDatasets">
          <el-option v-for="psu in psus" :key="psu.id" :label="psu.name" :value="psu.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="测试集">
        <el-select v-model="form.datasetId" placeholder="请选择测试集" style="width: 260px" clearable>
          <el-option v-for="dataset in datasets" :key="dataset.id" :label="dataset.name" :value="dataset.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="loadTaskHistory">筛选任务</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>任务历史</span>
          <el-button link type="primary" @click="loadTaskHistory">刷新</el-button>
        </div>
      </template>
      <el-table :data="taskList" v-loading="historyLoading">
        <el-table-column prop="id" label="任务ID" width="100" />
        <el-table-column prop="status" label="状态" width="150">
          <template #default="{ row }">
            <el-tag>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCases" label="总数" width="90" />
        <el-table-column prop="processedCases" label="已处理" width="90" />
        <el-table-column prop="averageScore" label="平均分" width="110" />
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="history-actions">
              <el-button type="primary" link @click="loadTaskDetail(row.id)">详情</el-button>
              <el-button type="success" link :disabled="!row.reportId" @click="loadReport(row.reportId)">报告</el-button>
              <el-button type="info" link @click="reuseTaskFilter(row)">回看</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="taskDetail.id" shadow="never" style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>任务详情 #{{ taskDetail.id }}</span>
          <el-tag>{{ taskDetail.status }}</el-tag>
        </div>
      </template>
      <div class="metric-grid">
        <div>总数：{{ taskDetail.totalCases }}</div>
        <div>已处理：{{ taskDetail.processedCases }}</div>
        <div>成功：{{ taskDetail.successCases }}</div>
        <div>失败：{{ taskDetail.failedCases }}</div>
        <div>平均分：{{ taskDetail.averageScore ?? '-' }}</div>
        <div>报告ID：{{ taskDetail.reportId ?? '-' }}</div>
      </div>
      <el-button v-if="taskDetail.reportId" type="primary" link @click="loadReport(taskDetail.reportId)">查看报告</el-button>
    </el-card>

    <el-card v-if="report.id" shadow="never" style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>评估报告 #{{ report.id }}</span>
          <span>总分：{{ report.overallScore ?? '-' }} / 通过率：{{ report.passRate ?? '-' }}%</span>
        </div>
      </template>
      <el-input v-model="report.summaryJson" type="textarea" :rows="6" readonly />
      <el-table :data="report.issueItems || []" style="margin-top: 12px">
        <el-table-column prop="caseId" label="用例ID" width="120" />
        <el-table-column prop="caseName" label="用例名称" width="180" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="reason" label="问题原因" min-width="240" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { evaluationApi, psuApi, testDatasetApi } from '@/services/api'

const route = useRoute()
const psus = ref([])
const datasets = ref([])
const currentTaskId = ref(null)
const historyLoading = ref(false)
const taskList = ref([])
const taskDetail = reactive({})
const report = reactive({})

const form = reactive({
  psuId: null,
  datasetId: null
})

const loadPsus = async () => {
  // 初始化PSU下拉，避免评估创建时无可选项。
  const res = await psuApi.getPsus(1, 200)
  psus.value = res.data?.content || []
}

const loadDatasets = async () => {
  if (!form.psuId) {
    datasets.value = []
    form.datasetId = null
    taskList.value = []
    return
  }
  const res = await testDatasetApi.getTestDatasets(form.psuId)
  datasets.value = res.data || []
  // PSU切换后若当前测试集不在新列表内，及时清空以避免误筛选。
  if (form.datasetId && !datasets.value.some(item => item.id === form.datasetId)) {
    form.datasetId = null
  }
  await loadTaskHistory(true)
}

const clearObject = (target) => {
  // 切换任务时先清空旧对象字段，避免页面残留脏数据。
  Object.keys(target).forEach((key) => {
    delete target[key]
  })
}

const loadTaskHistory = async (silent = false) => {
  if (!form.psuId) {
    taskList.value = []
    if (!silent) {
      ElMessage.warning('请先选择PSU')
    }
    return
  }
  historyLoading.value = true
  try {
    // 历史列表支持按测试集筛选，便于快速回看同一数据集任务。
    const res = await evaluationApi.getTasks(form.psuId, form.datasetId || null)
    taskList.value = res.data || []
  } finally {
    historyLoading.value = false
  }
}

const createTask = async (silent = false) => {
  if (!form.psuId || !form.datasetId) {
    if (!silent) {
      ElMessage.warning('请先选择PSU和测试集')
    }
    return
  }
  const res = await evaluationApi.createTask({
    psuId: form.psuId,
    datasetId: form.datasetId,
    dimensions: ['relevance', 'completeness', 'format']
  })
  clearObject(taskDetail)
  Object.assign(taskDetail, res.data || {})
  currentTaskId.value = taskDetail.id
  await loadTaskHistory(true)
  if (!silent) {
    ElMessage.success('评估任务创建成功')
  }
}

const runTask = async () => {
  if (!currentTaskId.value) {
    ElMessage.warning('请先创建评估任务')
    return
  }
  const res = await evaluationApi.runTask(currentTaskId.value)
  clearObject(taskDetail)
  Object.assign(taskDetail, res.data || {})
  await loadTaskHistory(true)
  ElMessage.success('评估任务执行完成')
  if (taskDetail.reportId) {
    await loadReport(taskDetail.reportId)
  }
}

const loadTaskDetail = async (taskId) => {
  // 统一通过详情接口回填当前任务，确保页面数据与后端一致。
  const res = await evaluationApi.getTask(taskId)
  clearObject(taskDetail)
  Object.assign(taskDetail, res.data || {})
  currentTaskId.value = taskDetail.id || null
}

const loadReport = async (reportId) => {
  if (!reportId) {
    return
  }
  const res = await evaluationApi.getReport(reportId)
  clearObject(report)
  Object.assign(report, res.data || {})
}

const reuseTaskFilter = async (row) => {
  // 回看时自动带入该任务筛选条件并加载详情/报告。
  form.psuId = row.psuId
  await loadDatasets()
  form.datasetId = row.datasetId
  await loadTaskHistory(true)
  await loadTaskDetail(row.id)
  if (row.reportId) {
    await loadReport(row.reportId)
  } else {
    clearObject(report)
  }
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  const time = new Date(value)
  if (Number.isNaN(time.getTime())) {
    return String(value)
  }
  return time.toLocaleString('zh-CN', { hour12: false })
}

const initFromRouteQuery = async () => {
  // 支持从测试集页面带参跳转，减少手工二次选择。
  const qPsuId = Number(route.query.psuId || 0)
  const qDatasetId = Number(route.query.datasetId || 0)
  if (qPsuId > 0) {
    form.psuId = qPsuId
    await loadDatasets()
  }
  if (qDatasetId > 0) {
    form.datasetId = qDatasetId
  }
  if (String(route.query.autoCreate || '') === '1' && form.psuId && form.datasetId) {
    await createTask(true)
    ElMessage.success('已根据测试集快速创建评估任务')
  }
}

onMounted(async () => {
  await loadPsus()
  await initFromRouteQuery()
  await loadTaskHistory(true)
})
</script>

<style scoped>
.evaluation-center {
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.actions {
  display: flex;
  gap: 8px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.history-actions {
  display: flex;
  gap: 4px;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 8px;
}
</style>
