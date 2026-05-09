import axios from 'axios'

const LEGACY_API_PREFIX = '/api'
const API_BASE_PREFIX = LEGACY_API_PREFIX

// 创建axios实例
const api = axios.create({
  baseURL: API_BASE_PREFIX,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export default api

// API接口定义
export const authApi = {
  // 登录
  login: (data) => api.post('/auth/login', data),
  // 获取用户信息
  getProfile: () => api.get('/auth/profile')
}

export const psuApi = {
  // 分页获取PSU列表
  getPsus: (page = 1, size = 10, name = '') => {
    const params = { page, size }
    if (name && String(name).trim()) {
      params.name = String(name).trim()
    }
    return api.get('/psus', { params })
  },
  // 根据数据库ID获取PSU
  getPsuById: (id) => api.get('/psus/by-id', { params: { id } }),
  // 根据PSU ID（全局唯一ID）+ 标签（FORMAL/PREVIEW）获取该标签版本的Schema与Prompt
  getPsuByPsuId: (psuId, tag) => api.get('/psus/by-psu-id/by-psuId', { params: { psuId, tag } }),
  // 获取PSU版本历史（按versionNo倒序）
  getPsuVersions: (psuId) => api.get('/psus/by-psuId/versions', { params: { psuId } }),
  // 获取可提交审核的PSU版本（后端判定）
  getSubmittablePsuVersions: (psuId) => api.get('/psus/by-psuId/submittable-versions', { params: { psuId } }),
  // 创建PSU
  createPsu: (data) => api.post('/psus', data),
  // 更新PSU
  updatePsu: (id, data) => api.post('/psus/by-id', data, { params: { id } }),
  // 删除PSU（归档）
  deletePsu: (id) => api.delete('/psus/by-id', { params: { id } })
}

export const schemaApi = {
  // 获取Schema
  getSchema: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get('/schemas/by-psuId', { params: { psuId } });
  },
  // 更新Schema
  updateSchema: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/schemas/by-psuId', data, { params: { psuId } });
  },
  // 兼容接口：覆盖写模式下仅返回当前Schema
  getSchemaVersions: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get('/schemas/by-psuId/versions', { params: { psuId } });
  }
}

export const paramSetApi = {
  // 获取参数集（覆盖写）
  getParamSet: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get('/param-sets/by-psuId', { params: { psuId } });
  },
  // 覆盖写参数集
  updateParamSet: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/param-sets/by-psuId', data, { params: { psuId } });
  }
}

export const promptApi = {
  // 获取Prompt片段
  getPromptFragments: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get('/prompts/by-psuId', { params: { psuId } });
  },
  // 创建Prompt片段（仅研发）
  createPromptFragment: (data) => api.post('/prompts', data),
  // 更新Prompt片段
  updatePromptFragment: (fragmentId, data) => api.post('/prompts/by-fragmentId', data, { params: { fragmentId } }),
  // 删除Prompt片段（仅研发）
  deletePromptFragment: (fragmentId) => api.delete('/prompts/by-fragmentId', { params: { fragmentId } }),
  // 定版Prompt片段（仅运营）
  finalizePromptFragment: (fragmentId) => api.post('/prompts/by-fragmentId/finalize', null, { params: { fragmentId } }),
  // 测试Prompt
  testPrompt: (psuId, data, requestConfig = {}) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/prompts/by-psuId/test', data, { ...requestConfig, params: { ...(requestConfig.params || {}), psuId } });
  }
}

export const versionApi = {
  // 提交版本审核
  submitVersion: (psuId, versionNo) => {
    const params = { psuId }
    if (versionNo !== undefined && versionNo !== null) {
      params.versionNo = versionNo
    }
    return api.post('/versions/by-psuId/submit', null, { params })
  },
  // 审核版本
  reviewVersion: (reviewId, data) => api.post('/versions/by-reviewId/review', data, { params: { reviewId } }),
  // 获取生成的代码
  getCode: (psuId, language = 'java') => api.get('/versions/by-psuId/code', { params: { psuId, language } })
}

export const userApi = {
  // 获取用户列表
  getUsers: () => api.get('/users'),
  // 创建用户
  createUser: (data) => api.post('/users', data),
  // 切换用户状态
  toggleUserStatus: (id) => api.post('/users/by-id/toggle-status', null, { params: { id } })
}

export const testDatasetApi = {
  // 获取PSU的测试数据集列表
  getTestDatasets: (psuId) => api.get('/test-datasets', { params: { psuId } }),
  // 创建测试数据集
  createTestDataset: (psuId, data) => api.post('/test-datasets', data, { params: { psuId } }),
  // 更新测试数据集
  updateTestDataset: (id, data) => api.post('/test-datasets/by-id', data, { params: { id } }),
  // 删除测试数据集
  deleteTestDataset: (id) => api.delete('/test-datasets/by-id', { params: { id } })
}

export const versionReviewApi = {
  // 获取版本审核列表（分页）
  getVersionReviews: (psuId, page = 1, size = 10) => {
    if (psuId && !isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    const params = { page, size };
    if (psuId) {
      params.psuId = psuId;
    }
    return api.get('/versions', { params })
  },
  // 获取特定版本审核记录
  getVersionReview: (reviewId) => {
    if (!isValidPsuId(reviewId)) {
      return Promise.reject(new Error(`Invalid Review ID: ${reviewId}`));
    }
    return api.get('/versions/by-id', { params: { id: reviewId } })
  },
  // 提交版本审核
  submitVersion: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/versions/by-psuId/submit', null, { params: { psuId } });
  },
  // 审核版本
  reviewVersion: (reviewId, data) => api.post('/versions/by-reviewId/review', data, { params: { reviewId } }),
  // 版本对比
  compareVersions: (psuId, fromVersionNo, toVersionNo) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get('/versions/by-psuId/compare', { params: { psuId, fromVersionNo, toVersionNo } });
  },
  // 版本回滚
  rollbackVersion: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/versions/by-psuId/rollback', data, { params: { psuId } });
  },
  // 获取生成的代码
  getCode: (psuId, language = 'java') => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get('/versions/by-psuId/code', { params: { psuId, language } });
  },
  // 登记Git提交哈希
  registerGitCommit: (reviewId, data) => {
    if (!isValidPsuId(reviewId)) {
      return Promise.reject(new Error(`Invalid Review ID: ${reviewId}`));
    }
    return api.post('/versions/by-reviewId/git-commit', data, { params: { reviewId } });
  }
  ,
  // 手动指定版本标签（FORMAL/PREVIEW）
  assignVersionTag: (reviewId, data) => {
    if (!isValidPsuId(reviewId)) {
      return Promise.reject(new Error(`Invalid Review ID: ${reviewId}`));
    }
    return api.post('/versions/by-reviewId/tag', data, { params: { reviewId } });
  }
  ,
  // 审核预览：按参数集渲染当前待审编排
  previewByParamSet: (reviewId) => api.get('/versions/by-reviewId/preview', { params: { reviewId } })
  ,
  // 获取审核版本快照详情（schema快照/编排快照）
  getReviewSnapshot: (reviewId) => api.get('/versions/by-reviewId/snapshot', { params: { reviewId } })
}

export const releaseApi = {
  // 发布单列表（分页）
  getReleases: (psuId, environment, page = 1, size = 10) => {
    const params = { page, size }
    if (psuId) params.psuId = psuId
    if (environment) params.environment = environment
    return api.get('/releases', { params })
  },
  // 发布单详情
  getRelease: (releaseId) => api.get('/releases/by-releaseId', { params: { releaseId } }),
  // 创建发布单
  createRelease: (data) => api.post('/releases', data),
  // 提交审核
  submitRelease: (releaseId) => api.post('/releases/by-releaseId/submit', null, { params: { releaseId } }),
  // 审核通过
  approveRelease: (releaseId) => api.post('/releases/by-releaseId/approve', null, { params: { releaseId } }),
  // 审核驳回
  rejectRelease: (releaseId, data) => api.post('/releases/by-releaseId/reject', data, { params: { releaseId } }),
  // 执行发布
  executeRelease: (releaseId) => api.post('/releases/by-releaseId/execute', null, { params: { releaseId } }),
  // 执行回滚
  rollbackRelease: (releaseId, data) => api.post('/releases/by-releaseId/rollback', data, { params: { releaseId } }),
  // 获取规则列表
  getReleaseRules: (releaseId) => api.get('/releases/by-releaseId/rules', { params: { releaseId } }),
  // 新增规则
  addReleaseRule: (releaseId, data) => api.post('/releases/by-releaseId/rules', data, { params: { releaseId } }),
  // 更新规则
  updateReleaseRule: (releaseId, ruleId, data) => api.post('/releases/by-releaseId/rules/by-ruleId', data, { params: { releaseId, ruleId } }),
  // 删除规则
  deleteReleaseRule: (releaseId, ruleId) => api.delete('/releases/by-releaseId/rules/by-ruleId', { params: { releaseId, ruleId } }),
  // 对外解析（联调验证）
  resolvePrompt: (data) => api.post('/prompt-service/resolve', data)
}

export const configApi = {
  // 获取所有系统配置
  getAllConfigs: () => api.get('/configs'),
  // 根据配置键获取配置
  getConfigByKey: (configKey) => api.get('/configs/by-configKey', { params: { configKey } }),
  // 获取DashScope测试用API Key
  getDashscopeKey: () => api.get('/configs/dashscope-key'),
  // 创建或更新系统配置
  saveConfig: (data) => api.post('/configs', data),
  // 删除系统配置
  deleteConfig: (id) => api.delete('/configs/by-id', { params: { id } })
}

export const auditLogApi = {
  // 获取所有审计日志
  getAllAuditLogs: () => api.get('/audit-logs'),
  // 根据用户ID获取审计日志
  getAuditLogsByUserId: (userId) => api.get('/audit-logs/user/by-userId', { params: { userId } })
}

export const compositionApi = {
  getComposition: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get('/compositions', { params: { psuId } });
  },
  saveDraft: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/compositions', data, { params: { psuId } });
  },
  validate: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/compositions/validate', data, { params: { psuId } });
  },
  render: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/compositions/render', data, { params: { psuId } });
  },
  submit: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/compositions/submit', null, { params: { psuId } });
  }
}

export const testRunApi = {
  runTest: (psuId, datasetId, data = {}) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post('/test-runs', data, { params: { psuId, datasetId } });
  },
  getTestRun: (runId) => api.get('/test-runs/by-runId', { params: { runId } }),
  getTestRuns: (psuId, datasetId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    const params = { psuId };
    if (datasetId) {
      params.datasetId = datasetId;
    }
    return api.get('/test-runs', { params });
  }
}

export const evaluationApi = {
  createTask: (data) => api.post('/evaluations/tasks', data),
  runTask: (taskId) => api.post('/evaluations/tasks/by-id/run', null, { params: { id: taskId } }),
  getTasks: (psuId, datasetId) => {
    const params = { psuId }
    if (datasetId) {
      params.datasetId = datasetId
    }
    return api.get('/evaluations/tasks', { params })
  },
  getTask: (taskId) => api.get('/evaluations/tasks/by-id', { params: { id: taskId } }),
  getReport: (reportId) => api.get('/evaluations/reports/by-id', { params: { id: reportId } })
}

// 验证PSU ID是否为有效的正整数
function isValidPsuId(psuId) {
  return Number.isInteger(Number(psuId)) && Number(psuId) > 0;
}
