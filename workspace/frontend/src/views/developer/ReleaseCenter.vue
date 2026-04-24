<template>
  <div class="release-center">
    <el-card>
      <template #header>
        <div class="header-row">
          <span class="title">发版中心</span>
          <el-button type="primary" @click="openCreateDialog">创建发布单</el-button>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="filterPsuId" placeholder="选择PSU" clearable style="width: 260px" @change="loadReleases">
          <el-option v-for="psu in psuList" :key="psu.id" :label="`${psu.name} (v${psu.versionNo ?? 1})`" :value="psu.id" />
        </el-select>
        <el-select v-model="filterEnv" placeholder="环境" clearable style="width: 160px" @change="loadReleases">
          <el-option label="DEV" value="DEV" />
          <el-option label="STAGING" value="STAGING" />
          <el-option label="PROD" value="PROD" />
        </el-select>
        <el-button @click="loadReleases">刷新</el-button>
      </div>

      <el-table :data="releases" border v-loading="loading" style="margin-top: 12px;">
        <el-table-column prop="id" label="发布单ID" width="100" />
        <el-table-column prop="psuId" label="PSU ID" width="100" />
        <el-table-column prop="environment" label="环境" width="110" />
        <el-table-column prop="releaseType" label="类型" width="110" />
        <el-table-column prop="targetCompositionId" label="编排ID" width="100" />
        <el-table-column prop="targetRevisionNo" label="目标版本" width="100" />
        <el-table-column prop="status" label="状态" width="140" />
        <el-table-column label="操作" min-width="420">
          <template #default="{ row }">
            <el-button size="small" @click="submit(row)" :disabled="row.status !== 'DRAFT'">提交</el-button>
            <el-button size="small" type="success" @click="approve(row)" :disabled="row.status !== 'PENDING_APPROVAL'">通过</el-button>
            <el-button size="small" type="warning" @click="reject(row)" :disabled="row.status !== 'PENDING_APPROVAL'">驳回</el-button>
            <el-button size="small" type="primary" @click="execute(row)" :disabled="row.status !== 'APPROVED'">执行</el-button>
            <el-button size="small" type="danger" @click="rollback(row)">回滚</el-button>
            <el-button size="small" @click="openRules(row)">规则</el-button>
            <el-button size="small" @click="resolveDebug(row)">解析</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="创建发布单" width="560px">
      <el-form :model="createForm" label-width="120px">
        <el-form-item label="PSU ID">
          <el-select v-model="createForm.psuId" placeholder="选择PSU">
            <el-option v-for="psu in psuList" :key="psu.id" :label="psu.name" :value="psu.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="createForm.environment">
            <el-option label="DEV" value="DEV" />
            <el-option label="STAGING" value="STAGING" />
            <el-option label="PROD" value="PROD" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布类型">
          <el-select v-model="createForm.releaseType">
            <el-option label="FULL" value="FULL" />
            <el-option label="CANARY" value="CANARY" />
          </el-select>
        </el-form-item>
        <el-form-item label="编排ID">
          <el-input-number v-model="createForm.targetCompositionId" :min="1" />
        </el-form-item>
        <el-form-item label="目标版本号">
          <el-input-number v-model="createForm.targetRevisionNo" :min="1" />
        </el-form-item>
        <el-form-item label="基线版本号">
          <el-input-number v-model="createForm.baseRevisionNo" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="createRelease">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rulesVisible" title="灰度规则" width="760px">
      <div style="margin-bottom: 12px;">
        <el-button type="primary" @click="openRuleEdit()">新增规则</el-button>
      </div>
      <el-table :data="rules" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="ruleType" label="类型" width="110" />
        <el-table-column prop="ruleKey" label="键" width="120" />
        <el-table-column prop="operator" label="操作符" width="100" />
        <el-table-column prop="ruleValue" label="值" />
        <el-table-column prop="trafficPercent" label="流量%" width="100" />
        <el-table-column prop="priority" label="优先级" width="90" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openRuleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="ruleEditVisible" :title="editingRule.id ? '编辑规则' : '新增规则'" width="520px">
      <el-form :model="editingRule" label-width="120px">
        <el-form-item label="规则类型">
          <el-select v-model="editingRule.ruleType">
            <el-option label="WHITELIST" value="WHITELIST" />
            <el-option label="TAG" value="TAG" />
            <el-option label="PERCENT" value="PERCENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则键">
          <el-input v-model="editingRule.ruleKey" />
        </el-form-item>
        <el-form-item label="操作符">
          <el-select v-model="editingRule.operator">
            <el-option label="EQ" value="EQ" />
            <el-option label="IN" value="IN" />
            <el-option label="REGEX" value="REGEX" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则值">
          <el-input v-model="editingRule.ruleValue" />
        </el-form-item>
        <el-form-item label="流量百分比">
          <el-input-number v-model="editingRule.trafficPercent" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="editingRule.priority" :min="1" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editingRule.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { psuApi, releaseApi } from '@/services/api'

const psuList = ref([])
const releases = ref([])
const loading = ref(false)
const filterPsuId = ref(null)
const filterEnv = ref('')
const createVisible = ref(false)
const rulesVisible = ref(false)
const selectedRelease = ref(null)
const rules = ref([])
const ruleEditVisible = ref(false)
const editingRule = ref({})

const createForm = ref({
  psuId: null,
  environment: 'PROD',
  releaseType: 'FULL',
  targetCompositionId: 1,
  targetRevisionNo: 1,
  baseRevisionNo: 1
})

const loadPsus = async () => {
  const res = await psuApi.getPsus(1, 200)
  psuList.value = res.data.content || []
}

const loadReleases = async () => {
  loading.value = true
  try {
    const res = await releaseApi.getReleases(filterPsuId.value, filterEnv.value, 1, 200)
    releases.value = res.data.content || []
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  createVisible.value = true
}

const createRelease = async () => {
  await releaseApi.createRelease(createForm.value)
  ElMessage.success('发布单创建成功')
  createVisible.value = false
  await loadReleases()
}

const submit = async (row) => {
  await releaseApi.submitRelease(row.id)
  ElMessage.success('提交成功')
  await loadReleases()
}

const approve = async (row) => {
  await releaseApi.approveRelease(row.id)
  ElMessage.success('审核通过')
  await loadReleases()
}

const reject = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回发布单', { inputType: 'text' })
  await releaseApi.rejectRelease(row.id, { rejectionReason: value || '驳回' })
  ElMessage.success('已驳回')
  await loadReleases()
}

const execute = async (row) => {
  await releaseApi.executeRelease(row.id)
  ElMessage.success('发布执行成功')
  await loadReleases()
}

const rollback = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入回滚目标 revisionNo', '回滚发布单', { inputType: 'number' })
  await releaseApi.rollbackRelease(row.id, { targetRevisionNo: Number(value), reason: '手工回滚' })
  ElMessage.success('回滚完成')
  await loadReleases()
}

const openRules = async (row) => {
  selectedRelease.value = row
  const res = await releaseApi.getReleaseRules(row.id)
  rules.value = res.data || []
  rulesVisible.value = true
}

const openRuleEdit = (row = null) => {
  editingRule.value = row
    ? { ...row }
    : { ruleType: 'PERCENT', ruleKey: 'tenantId', operator: 'EQ', ruleValue: '', trafficPercent: 10, priority: 100, enabled: true }
  ruleEditVisible.value = true
}

const saveRule = async () => {
  if (!selectedRelease.value) return
  if (editingRule.value.id) {
    await releaseApi.updateReleaseRule(selectedRelease.value.id, editingRule.value.id, editingRule.value)
  } else {
    await releaseApi.addReleaseRule(selectedRelease.value.id, editingRule.value)
  }
  ElMessage.success('规则保存成功')
  ruleEditVisible.value = false
  await openRules(selectedRelease.value)
}

const deleteRule = async (row) => {
  if (!selectedRelease.value) return
  await releaseApi.deleteReleaseRule(selectedRelease.value.id, row.id)
  ElMessage.success('规则删除成功')
  await openRules(selectedRelease.value)
}

const resolveDebug = async (row) => {
  const payload = {
    psuId: row.psuId,
    environment: row.environment,
    context: { tenantId: 't-a', userId: 'u-1', traceId: `trace-${Date.now()}` }
  }
  const res = await releaseApi.resolvePrompt(payload)
  ElMessage.success(`命中${res.data.routeType} revision=${res.data.revisionNo}`)
}

onMounted(async () => {
  await loadPsus()
  await loadReleases()
})
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.filters {
  display: flex;
  gap: 12px;
}
</style>
