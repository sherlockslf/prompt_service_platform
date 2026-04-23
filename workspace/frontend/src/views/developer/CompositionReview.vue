<template>
  <div class="composition-review">
    <el-page-header content="编排审核快照" @back="$router.push('/developer')" />
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
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { compositionApi, versionReviewApi } from '@/services/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const psuId = computed(() => Number(route.params.psuId))
const reviewId = computed(() => Number(route.params.reviewId))

const loading = ref(false)
const review = ref(null)
const content = ref('')
const compositionStatus = ref('DRAFT')

const statusTagType = computed(() => {
  const map = { DRAFT: 'info', SUBMITTED: 'warning', DEV_REVIEWING: 'primary', APPROVED: 'success', REJECTED: 'danger' }
  return map[compositionStatus.value] || 'info'
})

const statusText = computed(() => {
  const map = { DRAFT: '草稿', SUBMITTED: '已提交', DEV_REVIEWING: '研发审核中', APPROVED: '已通过', REJECTED: '已驳回' }
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
