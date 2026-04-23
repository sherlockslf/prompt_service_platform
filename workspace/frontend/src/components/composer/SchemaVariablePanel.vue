<template>
  <div class="schema-variable-panel">
    <h4>Schema 字段树</h4>
    <el-input
      v-model="localSearch"
      placeholder="搜索字段..."
      clearable
      size="small"
      style="margin-bottom: 12px"
      @update:model-value="$emit('update:search-query', $event)"
    />
    <el-tree
      :data="treeData"
      :props="{ label: 'label', children: 'children' }"
      :filter-node-method="filterNode"
      :expand-on-click-node="false"
      node-key="path"
      default-expand-all
      ref="treeRef"
    >
      <template #default="{ node, data }">
        <div class="tree-node" @click="handleClick(data)">
          <span class="node-label">{{ data.label }}</span>
          <span v-if="data.type" class="node-type">{{ data.type }}</span>
          <span v-if="data.required" class="node-required">必填</span>
        </div>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  schemaFields: { type: Array, default: () => [] },
  searchQuery: { type: String, default: '' }
})

const emit = defineEmits(['insert-variable', 'update:search-query'])

const localSearch = ref(props.searchQuery)
const treeRef = ref(null)

watch(() => props.searchQuery, (val) => {
  localSearch.value = val
  if (treeRef.value) treeRef.value.filter(val)
})

const treeData = computed(() => {
  return props.schemaFields.map(field => ({
    label: field.name,
    path: field.name,
    type: field.type,
    required: field.required,
    description: field.description,
    isLeaf: true
  }))
})

function filterNode(value, data) {
  if (!value) return true
  return data.label.toLowerCase().includes(value.toLowerCase())
}

function handleClick(data) {
  if (data.isLeaf) {
    emit('insert-variable', { name: data.path })
  }
}
</script>

<style scoped>
.schema-variable-panel h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #303133;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 0;
  width: 100%;
}

.tree-node:hover {
  background: #ecf5ff;
  border-radius: 4px;
}

.node-label {
  font-size: 13px;
  color: #303133;
}

.node-type {
  font-size: 11px;
  color: #909399;
  background: #f0f0f0;
  padding: 1px 4px;
  border-radius: 2px;
}

.node-required {
  font-size: 11px;
  color: #f56c6c;
}
</style>
