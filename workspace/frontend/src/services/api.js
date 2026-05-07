import axios from 'axios'

const LEGACY_API_PREFIX = '/api'
const VERSIONED_API_PREFIX = '/api/v1'
// 默认保持旧路径兼容；需要联调新路径时可配置 VITE_API_USE_V1=true。
const USE_VERSIONED_API = String(import.meta.env.VITE_API_USE_V1 || 'false').toLowerCase() === 'true'
const API_BASE_PREFIX = USE_VERSIONED_API ? VERSIONED_API_PREFIX : LEGACY_API_PREFIX

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
  getPsus: (page = 1, size = 10) => api.get('/psus', { params: { page, size } }),
  // 根据数据库ID获取PSU
  getPsuById: (id) => api.get(`/psus/${id}`),
  // 根据PSU ID（全局唯一ID）获取PSU
  getPsuByPsuId: (psuId) => api.get(`/psus/by-psu-id/${psuId}`),
  // 创建PSU
  createPsu: (data) => api.post('/psus', data),
  // 更新PSU
  updatePsu: (id, data) => api.put(`/psus/${id}`, data),
  // 删除PSU（归档）
  deletePsu: (id) => api.delete(`/psus/${id}`)
}

export const schemaApi = {
  // 获取Schema
  getSchema: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get(`/schemas/${psuId}`);
  },
  // 更新Schema
  updateSchema: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.put(`/schemas/${psuId}`, data);
  },
  // 兼容接口：覆盖写模式下仅返回当前Schema
  getSchemaVersions: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get(`/schemas/${psuId}/versions`);
  }
}

export const paramSetApi = {
  // 获取参数集（覆盖写）
  getParamSet: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get(`/param-sets/${psuId}`);
  },
  // 覆盖写参数集
  updateParamSet: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.put(`/param-sets/${psuId}`, data);
  }
}

export const promptApi = {
  // 获取Prompt片段
  getPromptFragments: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get(`/prompts/${psuId}`);
  },
  // 创建Prompt片段（仅研发）
  createPromptFragment: (data) => api.post('/prompts', data),
  // 更新Prompt片段
  updatePromptFragment: (fragmentId, data) => api.put(`/prompts/${fragmentId}`, data),
  // 删除Prompt片段（仅研发）
  deletePromptFragment: (fragmentId) => api.delete(`/prompts/${fragmentId}`),
  // 定版Prompt片段（仅运营）
  finalizePromptFragment: (fragmentId) => api.post(`/prompts/${fragmentId}/finalize`),
  // 测试Prompt
  testPrompt: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post(`/prompts/${psuId}/test`, data);
  }
}

export const versionApi = {
  // 提交版本审核
  submitVersion: (psuId) => api.post(`/versions/${psuId}/submit`),
  // 审核版本
  reviewVersion: (reviewId, data) => api.post(`/versions/${reviewId}/review`, data),
  // 获取生成的代码
  getCode: (psuId, language = 'java') => api.get(`/versions/${psuId}/code`, { params: { language } })
}

export const userApi = {
  // 获取用户列表
  getUsers: () => api.get('/users'),
  // 创建用户
  createUser: (data) => api.post('/users', data),
  // 切换用户状态
  toggleUserStatus: (id) => api.put(`/users/${id}/toggle-status`)
}

export const testDatasetApi = {
  // 获取PSU的测试数据集列表
  getTestDatasets: (psuId) => api.get('/test-datasets', { params: { psuId } }),
  // 创建测试数据集
  createTestDataset: (psuId, data) => api.post('/test-datasets', data, { params: { psuId } }),
  // 更新测试数据集
  updateTestDataset: (id, data) => api.put(`/test-datasets/${id}`, data),
  // 删除测试数据集
  deleteTestDataset: (id) => api.delete(`/test-datasets/${id}`)
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
    return api.get(`/versions/${reviewId}`)
  },
  // 提交版本审核
  submitVersion: (psuId) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post(`/versions/${psuId}/submit`);
  },
  // 审核版本
  reviewVersion: (reviewId, data) => api.post(`/versions/${reviewId}/review`, data),
  // 版本对比
  compareVersions: (psuId, fromVersionNo, toVersionNo) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get(`/versions/${psuId}/compare`, { params: { fromVersionNo, toVersionNo } });
  },
  // 版本回滚
  rollbackVersion: (psuId, data) => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.post(`/versions/${psuId}/rollback`, data);
  },
  // 获取生成的代码
  getCode: (psuId, language = 'java') => {
    if (!isValidPsuId(psuId)) {
      return Promise.reject(new Error(`Invalid PSU ID: ${psuId}`));
    }
    return api.get(`/versions/${psuId}/code`, { params: { language } });
  },
  // 登记Git提交哈希
  registerGitCommit: (reviewId, data) => {
    if (!isValidPsuId(reviewId)) {
      return Promise.reject(new Error(`Invalid Review ID: ${reviewId}`));
    }
    return api.post(`/versions/${reviewId}/git-commit`, data);
  }
  ,
  // 手动指定版本标签（FORMAL/PREVIEW）
  assignVersionTag: (reviewId, data) => {
    if (!isValidPsuId(reviewId)) {
      return Promise.reject(new Error(`Invalid Review ID: ${reviewId}`));
    }
    return api.post(`/versions/${reviewId}/tag`, data);
  }
  ,
  // 审核预览：按参数集渲染当前待审编排
  previewByParamSet: (reviewId) => api.get(`/versions/${reviewId}/preview`)
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
  getRelease: (releaseId) => api.get(`/releases/${releaseId}`),
  // 创建发布单
  createRelease: (data) => api.post('/releases', data),
  // 提交审核
  submitRelease: (releaseId) => api.post(`/releases/${releaseId}/submit`),
  // 审核通过
  approveRelease: (releaseId) => api.post(`/releases/${releaseId}/approve`),
  // 审核驳回
  rejectRelease: (releaseId, data) => api.post(`/releases/${releaseId}/reject`, data),
  // 执行发布
  executeRelease: (releaseId) => api.post(`/releases/${releaseId}/execute`),
  // 执行回滚
  rollbackRelease: (releaseId, data) => api.post(`/releases/${releaseId}/rollback`, data),
  // 获取规则列表
  getReleaseRules: (releaseId) => api.get(`/releases/${releaseId}/rules`),
  // 新增规则
  addReleaseRule: (releaseId, data) => api.post(`/releases/${releaseId}/rules`, data),
  // 更新规则
  updateReleaseRule: (releaseId, ruleId, data) => api.put(`/releases/${releaseId}/rules/${ruleId}`, data),
  // 删除规则
  deleteReleaseRule: (releaseId, ruleId) => api.delete(`/releases/${releaseId}/rules/${ruleId}`),
  // 对外解析（联调验证）
  resolvePrompt: (data) => api.post('/prompt-service/resolve', data)
}

export const configApi = {
  // 获取所有系统配置
  getAllConfigs: () => api.get('/configs'),
  // 根据配置键获取配置
  getConfigByKey: (configKey) => api.get(`/configs/${configKey}`),
  // 获取DashScope测试用API Key
  getDashscopeKey: () => api.get('/configs/dashscope-key'),
  // 创建或更新系统配置
  saveConfig: (data) => api.post('/configs', data),
  // 删除系统配置
  deleteConfig: (id) => api.delete(`/configs/${id}`)
}

export const auditLogApi = {
  // 获取所有审计日志
  getAllAuditLogs: () => api.get('/audit-logs'),
  // 根据用户ID获取审计日志
  getAuditLogsByUserId: (userId) => api.get(`/audit-logs/user/${userId}`)
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
    return api.put('/compositions', data, { params: { psuId } });
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
  getTestRun: (runId) => api.get(`/test-runs/${runId}`),
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
  runTask: (taskId) => api.post(`/evaluations/tasks/${taskId}/run`),
  getTasks: (psuId, datasetId) => {
    const params = { psuId }
    if (datasetId) {
      params.datasetId = datasetId
    }
    return api.get('/evaluations/tasks', { params })
  },
  getTask: (taskId) => api.get(`/evaluations/tasks/${taskId}`),
  getReport: (reportId) => api.get(`/evaluations/reports/${reportId}`)
}

// 验证PSU ID是否为有效的正整数
function isValidPsuId(psuId) {
  return Number.isInteger(Number(psuId)) && Number(psuId) > 0;
}
