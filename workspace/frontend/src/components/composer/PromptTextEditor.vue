<template>
  <div class="prompt-text-editor">
    <div class="editor-header">
      <span class="editor-title">编辑器</span>
      <span v-if="readonly" class="readonly-badge">只读</span>
    </div>
    <el-input
      v-model="localContent"
      type="textarea"
      :rows="20"
      :disabled="readonly"
      placeholder="请输入 Prompt 内容，使用 {{变量名}} 插入变量占位符"
      @update:model-value="onInput"
      ref="textareaRef"
    />
    <div class="editor-footer">
      <span class="char-count">字符数: {{ localContent?.length || 0 }}</span>
      <span class="var-count">变量数: {{ varCount }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  content: { type: String, default: '' },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:content'])
const textareaRef = ref(null)
const localContent = ref(props.content)

watch(() => props.content, (val) => {
  localContent.value = val
})

const varCount = computed(() => {
  if (!localContent.value) return 0
  const matches = localContent.value.match(/\{\{\s*[^}]+\s*\}\}/g)
  return matches ? matches.length : 0
})

function onInput(val) {
  emit('update:content', val)
}

function insertAtCursor(text) {
  const textarea = textareaRef.value?.$el?.querySelector('textarea')
  if (!textarea) {
    localContent.value += text
    emit('update:content', localContent.value)
    return
  }

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const before = localContent.value.substring(0, start)
  const after = localContent.value.substring(end)
  localContent.value = before + text + after
  emit('update:content', localContent.value)

  setTimeout(() => {
    textarea.focus()
    const newPos = start + text.length
    textarea.setSelectionRange(newPos, newPos)
  }, 0)
}

defineExpose({ insertAtCursor })
</script>

<style scoped>
.prompt-text-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

.editor-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.readonly-badge {
  font-size: 12px;
  color: #f56c6c;
  background: #fef0f0;
  padding: 2px 8px;
  border-radius: 4px;
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  padding: 6px 12px;
  background: #f5f7fa;
  border-top: 1px solid #dcdfe6;
  font-size: 12px;
  color: #909399;
}
</style>
