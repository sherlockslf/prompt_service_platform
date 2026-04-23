<template>
  <div class="injection-summary-panel">
    <h4>变量清单</h4>
    <el-table :data="injectionPlan" size="small" style="width: 100%; margin-bottom: 12px" max-height="200">
      <el-table-column prop="path" label="变量路径" min-width="120" />
      <el-table-column label="必填" width="50" align="center">
        <template #default="{ row }">
          <el-tag :type="row.required ? 'danger' : 'success'" size="small">
            {{ row.required ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="次数" width="50" align="center">
        <template #default="{ row }">
          {{ row.count || 1 }}
        </template>
      </el-table-column>
    </el-table>

    <div v-if="injectionPlan.length === 0" class="empty-hint">
      暂无变量，请从左侧 Schema 字段树插入变量
    </div>
    <div v-else class="quick-hints">
      <div v-if="invalidVars.length > 0" class="error-item">
        <el-icon color="#f56c6c"><CircleCloseFilled /></el-icon>
        <span>存在未在Schema中定义的变量: {{ invalidVars.map(v => v.path).join(', ') }}</span>
      </div>
      <div v-if="duplicateVars.length > 0" class="warning-item">
        <el-icon color="#e6a23c"><WarningFilled /></el-icon>
        <span>存在重复引用变量: {{ duplicateVars.map(v => `${v.path}(${v.count})`).join(', ') }}</span>
      </div>
    </div>

    <h4 style="margin-top: 16px">校验结果</h4>
    <div v-if="validationErrors.length > 0" class="error-list">
      <div v-for="(err, idx) in validationErrors" :key="'err-' + idx" class="error-item">
        <el-icon color="#f56c6c"><CircleCloseFilled /></el-icon>
        <span>{{ err.message }}</span>
      </div>
    </div>
    <div v-if="validationWarnings.length > 0" class="warning-list">
      <div v-for="(warn, idx) in validationWarnings" :key="'warn-' + idx" class="warning-item">
        <el-icon color="#e6a23c"><WarningFilled /></el-icon>
        <span>{{ warn.message }}</span>
      </div>
    </div>
    <div v-if="validationErrors.length === 0 && validationWarnings.length === 0" class="empty-hint">
      点击"校验"按钮进行校验
    </div>

    <h4 style="margin-top: 16px">Schema 字段状态</h4>
    <div class="schema-status">
      <div v-for="field in schemaFields" :key="field.name" class="schema-field-status">
        <span :class="['field-name', isUsed(field.name) ? 'used' : 'unused']">
          {{ field.name }}
        </span>
        <el-tag :type="isUsed(field.name) ? 'success' : 'info'" size="small">
          {{ isUsed(field.name) ? '已引用' : '未引用' }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCloseFilled, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  tokens: { type: Array, default: () => [] },
  injectionPlan: { type: Array, default: () => [] },
  validationErrors: { type: Array, default: () => [] },
  validationWarnings: { type: Array, default: () => [] },
  schemaFields: { type: Array, default: () => [] }
})

const invalidVars = computed(() => {
  const rootNames = new Set((props.schemaFields || []).map(f => f.name))
  return (props.injectionPlan || []).filter(item => {
    const path = String(item?.path || '')
    const root = path.split(/[.\[]/)[0]
    return path && !rootNames.has(root)
  })
})

const duplicateVars = computed(() => {
  return (props.injectionPlan || []).filter(item => Number(item?.count || 0) > 1)
})

function isUsed(fieldName) {
  return (props.injectionPlan || []).some(item =>
    item.path === fieldName || item.path.startsWith(fieldName + '.') || item.path.startsWith(fieldName + '[')
  )
}
</script>

<style scoped>
.injection-summary-panel h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}

.empty-hint {
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding: 12px 0;
}

.error-list, .warning-list {
  margin-bottom: 8px;
}

.error-item, .warning-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  padding: 4px 0;
}

.error-item {
  color: #f56c6c;
}

.warning-item {
  color: #e6a23c;
}

.schema-status {
  max-height: 200px;
  overflow-y: auto;
}

.schema-field-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
}

.field-name {
  font-size: 12px;
}

.field-name.used {
  color: #67c23a;
}

.field-name.unused {
  color: #909399;
}
</style>
