<template>
  <div class="composition-review">
    <el-page-header content="编排审核快照" @back="goBackToReviewList" />
    <el-card style="margin-top: 16px" v-loading="loading">
      <template #header>
        <div class="header-row">
          <span>PSU: {{ psuId }}</span>
          <el-tag :type="statusTagType">{{ statusText }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="审核ID">{{ review?.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ review?.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ review?.submittedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ review?.reviewedAt || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin-top: 16px">编排内容（只读）</h4>
      <el-input v-model="content" type="textarea" :rows="12" readonly />

      <h4 style="margin-top: 16px">参数集预览效果</h4>
      <el-button type="primary" size="small" :loading="previewing" @click="loadPreviewByParamSet">按参数集渲染</el-button>
      <el-alert
        v-if="missingVars.length"
        style="margin-top: 8px"
        type="warning"
        :closable="false"
        :title="`缺失变量: ${missingVars.join(', ')}`"
      />
      <el-input v-model="previewPrompt" style="margin-top: 8px" type="textarea" :rows="8" readonly />
      <h4 style="margin-top: 16px">参数集快照</h4>
      <el-input v-model="paramSetSnapshotText" type="textarea" :rows="8" readonly />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { compositionApi, versionReviewApi } from '@/services/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const psuId = computed(() => Number(route.query.psuId))
const reviewId = computed(() => Number(route.query.reviewId))

const loading = ref(false)
const previewing = ref(false)
const review = ref(null)
const content = ref('')
const compositionStatus = ref('DRAFT')
const previewPrompt = ref('')
const missingVars = ref([])
const paramSetSnapshotText = ref('{}')

const statusTagType = computed(() => {
  const map = { DRAFT: 'info', CANDIDATE: 'warning', FORMAL: 'success', ARCHIVED: 'info' }
  return map[compositionStatus.value] || 'info'
})

const statusText = computed(() => {
  const map = { DRAFT: '草稿', CANDIDATE: '发布候选', FORMAL: '正式版本', ARCHIVED: '归档' }
  return map[compositionStatus.value] || compositionStatus.value
})

onMounted(async () => {
  loading.value = true
  try {
    const [reviewRes, compositionRes] = await Promise.all([
      versionReviewApi.getVersionReviews(psuId.value, 1, 100),
      compositionApi.getComposition(psuId.value)
    ])
    review.value = (reviewRes.data.content || []).find(item => item.id === reviewId.value) || null
    content.value = compositionRes.data?.content || ''
    compositionStatus.value = compositionRes.data?.status || 'DRAFT'
  } catch (e) {
    ElMessage.error('加载审核信息失败')
  } finally {
    loading.value = false
  }
})

const loadPreviewByParamSet = async () => {
  previewing.value = true
  try {
    const res = await versionReviewApi.previewByParamSet(reviewId.value)
    previewPrompt.value = res.data?.renderedPrompt || ''
    missingVars.value = Array.isArray(res.data?.missingVars) ? res.data.missingVars : []
    paramSetSnapshotText.value = formatJsonText(res.data?.paramSetSnapshot || {})
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '按参数集渲染失败')
  } finally {
    previewing.value = false
  }
}

const goBackToReviewList = () => {
  // 返回版本审核菜单并携带定位参数，方便回到当前审核单据
  router.push(`/developer?menu=4&psuId=${psuId.value}&reviewId=${reviewId.value}`)
}

const formatJsonText = (data) => {
  if (!data) return '{}'
  try {
    return JSON.stringify(data, null, 2)
  } catch (e) {
    return String(data)
  }
}
</script>

<style scoped>
.composition-review {
  padding: 20px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
