<template>
  <div class="prompt-management-business">
    <el-card class="card-container">
      <template #header>
        <div class="card-header">
          <span class="title">Prompt编排调试</span>
        </div>
      </template>

      <!-- PSU选择 -->
      <div class="psu-selection">
        <el-select v-model="selectedPsuId" placeholder="请选择PSU" @change="onPsuChange" style="width: 400px;">
          <el-option
            v-for="psu in psuList"
            :key="psu.id"
            :label="`${psu.name} (v${psu.versionNo ?? 1})`"
            :value="psu.id"
          />
        </el-select>
      </div>

      <!-- Schema信息展示 -->
      <el-card v-if="selectedPsuId && schemaInfo" class="schema-info" style="margin-top: 20px;">
        <template #header>
          <span>Schema结构信息</span>
        </template>
        <div class="schema-content">
          <SchemaVariablePanel
            :schema-fields="schemaFields"
            :search-query="varSearch"
            @insert-variable="insertVariable"
            @update:search-query="varSearch = $event"
          />
        </div>
      </el-card>

      <!-- Prompt片段编辑区域 -->
      <div v-if="selectedPsuId" class="prompt-edit-area" style="margin-top: 20px;">
        <h3>Prompt片段列表</h3>
        <div v-for="fragment in promptFragments" :key="fragment.id" class="fragment-item">
          <el-card>
            <template #header>
              <div class="fragment-header">
                <span class="fragment-title">{{ fragment.fragmentKey }} ({{ formatTypeLabel(fragment.type) }})</span>
                <div class="fragment-actions">
                  <el-tag :type="fragment.editable ? 'success' : 'info'" style="margin-right: 10px;">
                    {{ fragment.editable ? '可编辑' : '已定版' }}
                  </el-tag>
                  <el-button 
                    v-if="fragment.editable" 
                    size="small" 
                    @click="openEditDialog(fragment)"
                    :disabled="selectedPsuStatus !== 'DRAFT'"
                  >
                    编辑
                  </el-button>
                  <el-button 
                    v-if="fragment.editable && !fragment.isFinalized" 
                    size="small" 
                    type="primary" 
                    @click="finalizePrompt(fragment)"
                    :disabled="selectedPsuStatus !== 'DRAFT'"
                  >
                    定版
                  </el-button>
                </div>
              </div>
            </template>
            <div class="fragment-content">
              <pre>{{ fragment.content }}</pre>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 编辑弹窗 -->
      <el-dialog
        v-model="editDialogVisible"
        title="编辑Prompt片段"
        width="70%"
      >
        <div v-if="currentFragment">
          <el-form :model="currentFragment" label-width="100px">
            <el-form-item label="片段标识">
              <el-input v-model="currentFragment.fragmentKey" disabled />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="currentFragment.type" disabled>
                <el-option label="核心规则片段" value="CORE_RULES" />
                <el-option label="消息模板片段" value="MESSAGE_TEMPLATE" />
              </el-select>
            </el-form-item>
            <el-form-item label="内容">
              <PromptTextEditor
                v-model:content="currentFragment.content"
                :readonly="!currentFragment.editable"
                ref="editorRef"
              />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="currentFragment.sortOrder" :min="0" disabled />
            </el-form-item>
          </el-form>
        </div>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button 
              v-if="currentFragment && currentFragment.editable" 
              type="primary" 
              @click="saveEditedFragment"
            >
              保存
            </el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 测试区域 -->
      <el-card v-if="selectedPsuId" class="test-card" style="margin-top: 20px;">
        <template #header>
          <span>在线调试测试</span>
        </template>
        <div class="test-section">
          <h4>输入测试参数</h4>
          <el-input
            v-model="testInput"
            type="textarea"
            :rows="6"
            placeholder="请输入测试参数(JSON格式)，例如：{&quot;productName&quot;: &quot;商品名称&quot;, &quot;userQuery&quot;: &quot;用户查询&quot;}"
          />
          <div style="margin-top: 15px;">
            <el-button type="primary" @click="testPrompt" :loading="isTesting">执行测试</el-button>
          </div>
          
          <div v-if="testResult" class="test-result">
            <h4>测试结果：</h4>
            <pre>{{ testResult }}</pre>
          </div>
        </div>
      </el-card>

      <!-- 提交审核 -->
      <el-card v-if="selectedPsuId" class="submit-card" style="margin-top: 20px;">
        <template #header>
          <span>版本提交审核</span>
        </template>
        <div class="submit-section">
          <p>当前PSU的所有Prompt片段调试完成后，可以提交给研发审核定版。</p>
          <el-button type="success" @click="submitForReview">提交审核</el-button>
        </div>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, promptApi, versionReviewApi, schemaApi } from '@/services/api'
import SchemaVariablePanel from '@/components/composer/SchemaVariablePanel.vue'
import PromptTextEditor from '@/components/composer/PromptTextEditor.vue'

// 响应式数据
const selectedPsuId = ref(null)
const selectedPsuStatus = ref('')
const psuList = ref([])
const promptFragments = ref([])
const schemaInfo = ref(null)
const schemaFields = ref([])
const editDialogVisible = ref(false)
const currentFragment = ref(null)
const testInput = ref('')
const testResult = ref('')
const isTesting = ref(false)
const varSearch = ref('')
const editorRef = ref(null)

// 获取PSU列表
const loadPsuList = async () => {
  try {
    const response = await api.get('/api/psus')
    psuList.value = response.data.content || response.data
  } catch (error) {
    console.error('获取PSU列表失败:', error)
    ElMessage.error('获取PSU列表失败')
  }
}

// PSU变化时加载对应的Prompt片段和Schema
const onPsuChange = async () => {
  selectedPsuStatus.value = (psuList.value.find(item => item.id === selectedPsuId.value)?.status) || ''
  if (selectedPsuId.value && isValidPsuId(selectedPsuId.value)) {
    await Promise.all([
      loadPromptFragments(),
      loadSchemaInfo(),
      loadSchemaFields()
    ])
  } else if (selectedPsuId.value) {
    ElMessage.error('无效的PSU ID，请选择一个有效的PSU');
    console.error('Invalid PSU ID:', selectedPsuId.value);
  }
}

// 加载Prompt片段
const loadPromptFragments = async () => {
  try {
    const response = await promptApi.getPromptFragments(selectedPsuId.value)
    promptFragments.value = response.data
  } catch (error) {
    console.error('获取Prompt片段失败:', error)
    ElMessage.error('获取Prompt片段失败')
  }
}

// 加载Schema信息
const loadSchemaInfo = async () => {
  try {
    const response = await api.get(`/api/schemas/${selectedPsuId.value}`)
    schemaInfo.value = response.data
  } catch (error) {
    console.error('获取Schema信息失败:', error)
    // Schema获取失败不影响Prompt编辑功能
  }
}

// 加载Schema字段信息
const loadSchemaFields = async () => {
  try {
    const response = await schemaApi.getSchema(selectedPsuId.value)
    let schemaContent = response.data.schemaContent || '{}'
    if (typeof schemaContent === 'string') {
      schemaContent = JSON.parse(schemaContent)
    }
    schemaFields.value = parseSchemaFields(schemaContent)
  } catch (error) {
    console.error('获取Schema字段失败:', error)
    schemaFields.value = []
  }
}

// 解析Schema字段(支持标准JSON Schema格式和扁平JSON格式)
function parseSchemaFields(schema) {
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
function inferType(value) {
  if (value === null || value === undefined) return 'null'
  if (Array.isArray(value)) return 'array'
  const type = typeof value
  if (type === 'number') return 'number'
  if (type === 'boolean') return 'boolean'
  if (type === 'object') return 'object'
  return 'string'
}

// 插入变量到编辑器
function insertVariable(field) {
  if (editorRef.value && typeof editorRef.value.insertAtCursor === 'function') {
    editorRef.value.insertAtCursor(`{{${field.name}}}`)
  } else {
    // 如果ref还没准备好，稍后再尝试
    nextTick(() => {
      if (editorRef.value && typeof editorRef.value.insertAtCursor === 'function') {
        editorRef.value.insertAtCursor(`{{${field.name}}}`)
      }
    })
  }
}

// 打开编辑对话框
const openEditDialog = (fragment) => {
  currentFragment.value = { ...fragment }
  editDialogVisible.value = true
}

// 保存编辑的片段
const saveEditedFragment = async () => {
  if (!currentFragment.value?.id) {
    ElMessage.warning('无效的片段ID')
    return
  }
  
  try {
    await promptApi.updatePromptFragment(currentFragment.value.id, {
      content: currentFragment.value.content
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await loadPromptFragments() // 重新加载列表
  } catch (error) {
    console.error('保存Prompt片段失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}

// 定版Prompt
const finalizePrompt = async (fragment) => {
  try {
    await ElMessageBox.confirm(
      `确定要定版 "${fragment.fragmentKey}" 吗？定版后将不可修改！`,
      '确认定版',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await promptApi.finalizePromptFragment(fragment.id)
    ElMessage.success('定版成功')
    await loadPromptFragments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('定版Prompt失败:', error)
      ElMessage.error(error.response?.data?.message || '定版失败')
    }
  }
}

// 测试Prompt
const testPrompt = async () => {
  try {
    isTesting.value = true
    let params = {}
    if (testInput.value.trim()) {
      params = JSON.parse(testInput.value)
    }
    
    const response = await promptApi.testPrompt(selectedPsuId.value, params)
    testResult.value = response.data
  } catch (error) {
    console.error('测试Prompt失败:', error)
    ElMessage.error('测试失败: ' + (error.response?.data?.message || error.message))
  } finally {
    isTesting.value = false
  }
}

// 提交审核
const submitForReview = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要提交当前版本进行审核吗？提交后研发将收到审核通知。',
      '提交审核',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    await versionReviewApi.submitVersion(selectedPsuId.value)
    ElMessage.success('提交审核成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交审核失败:', error)
      ElMessage.error(error.response?.data?.message || '提交审核失败')
    }
  }
}

// 验证PSU ID是否为有效的正整数
function isValidPsuId(psuId) {
  return Number.isInteger(Number(psuId)) && Number(psuId) > 0;
}

// 格式化类型标签
const formatTypeLabel = (type) => {
  switch (type) {
    case 'CORE_RULES':
      return '核心规则'
    case 'MESSAGE_TEMPLATE':
      return '消息模板'
    default:
      return type
  }
}

// 初始化数据
onMounted(async () => {
  await loadPsuList()
})
</script>

<style scoped>
.card-container {
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.psu-selection {
  margin-bottom: 20px;
}

.schema-info {
  font-family: monospace;
}

.schema-content {
  max-height: 300px;
  overflow-y: auto;
}

.fragment-item {
  margin-bottom: 15px;
}

.fragment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.fragment-title {
  font-weight: bold;
}

.fragment-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.test-card, .submit-card {
  margin-top: 20px;
}

.test-section, .submit-section {
  padding: 10px 0;
}

.test-result {
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
  overflow-x: auto;
}

.dialog-footer {
  text-align: right;
}

/* 新增样式 */
.variable-panel {
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fafafa;
}

.variable-item {
  padding: 8px 12px;
  margin: 5px 0;
  background-color: white;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.variable-item:hover {
  background-color: #f0f9ff;
  border-color: #b3d8ff;
}

.variable-name {
  font-weight: bold;
  color: #409eff;
}

.variable-description {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.search-box {
  margin-bottom: 15px;
}

.insert-btn {
  margin-left: 10px;
  font-size: 12px;
}
</style>
