-- PSU全生命周期管理平台数据库初始化脚本

-- 创建用户表
CREATE TABLE IF NOT EXISTS ai_prompt_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '明文密码',
    role ENUM('ADMIN', 'DEVELOPER', 'BUSINESS') NOT NULL COMMENT '角色',
    enabled TINYINT(1) DEFAULT 1 COMMENT '启用状态: 1-启用, 0-停用',
    phone_number VARCHAR(20) COMMENT '手机号码',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_users_username (username),
    INDEX idx_users_role (role),
    INDEX idx_users_enabled (enabled)
) COMMENT 'AI Prompt用户表';

-- 创建PSU单元表
CREATE TABLE IF NOT EXISTS ai_prompt_psu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id VARCHAR(100) NOT NULL UNIQUE COMMENT '全局唯一PSU ID',
    name VARCHAR(200) NOT NULL COMMENT 'PSU名称',
    description TEXT COMMENT '描述',
    status ENUM('DRAFT', 'CANDIDATE', 'FORMAL', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT' COMMENT '生命周期状态：草稿/候选/正式/归档',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    version_no INT NOT NULL DEFAULT 1 COMMENT 'PSU版本号（独立递增）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_psu_units_psu_id (psu_id),
    INDEX idx_psu_units_status (status),
    INDEX idx_psu_units_creator (creator_id),
    INDEX idx_psu_units_created (created_at)
) COMMENT 'AI Prompt PSU单元表';

-- 创建JSON Schema表
CREATE TABLE IF NOT EXISTS ai_prompt_json_schemas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    schema_content JSON NOT NULL COMMENT 'JSON Schema内容',
    version INT NOT NULL DEFAULT 1 COMMENT '兼容字段：覆盖写语义下固定为1',
    modified_by BIGINT NOT NULL COMMENT '修改者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    change_log TEXT COMMENT '变更日志',
    
    INDEX idx_json_schemas_psu_id (psu_id),
    INDEX idx_json_schemas_modified_by (modified_by),
    INDEX idx_json_schemas_created (created_at),
    
    CONSTRAINT uk_json_schemas_psu_id UNIQUE (psu_id)
) COMMENT 'AI Prompt JSON Schema表';

-- 创建参数集表（覆盖写语义）
CREATE TABLE IF NOT EXISTS ai_prompt_param_sets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    param_set_content JSON NOT NULL COMMENT '参数集内容',
    modified_by BIGINT NOT NULL COMMENT '修改者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    change_log TEXT COMMENT '变更日志',
    
    INDEX idx_param_sets_psu_id (psu_id),
    INDEX idx_param_sets_modified_by (modified_by),
    INDEX idx_param_sets_created (created_at),
    
    CONSTRAINT uk_param_sets_psu_id UNIQUE (psu_id)
) COMMENT 'AI Prompt参数集表';

-- 创建Prompt片段表
CREATE TABLE IF NOT EXISTS ai_prompt_prompt_fragments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    fragment_key VARCHAR(100) NOT NULL COMMENT '片段标识',
    content TEXT NOT NULL COMMENT 'Prompt内容',
    editable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可编辑: 0-锁定, 1-可编辑',
    type ENUM('CORE_RULES', 'MESSAGE_TEMPLATE') NOT NULL COMMENT '类型',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_prompt_fragments_psu_id (psu_id),
    INDEX idx_prompt_fragments_type (type),
    INDEX idx_prompt_fragments_editable (editable),
    INDEX idx_prompt_fragments_sort (sort_order),
    
    CONSTRAINT uk_prompt_fragments_psu_key UNIQUE (psu_id, fragment_key)
) COMMENT 'AI Prompt片段表';

-- 创建版本审核表
CREATE TABLE IF NOT EXISTS ai_prompt_version_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号（单字段递增）',
    status ENUM('DRAFT', 'CANDIDATE', 'FORMAL', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态：草稿/候选/正式/归档',
    submitter_id BIGINT NOT NULL COMMENT '提交者ID',
    reviewer_id BIGINT COMMENT '审核者ID',
    rejection_reason TEXT COMMENT '驳回原因',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    reviewed_at DATETIME COMMENT '审核时间',
    git_commit_hash VARCHAR(64) COMMENT 'Git提交哈希',
    code_content LONGTEXT COMMENT '生成的代码内容',
    composition_id BIGINT NULL COMMENT '关联编排ID',
    composition_revision_no INT NULL COMMENT '关联编排快照版本',
    rejection_type ENUM('BACK_TO_DEV', 'BACK_TO_BIZ') NULL COMMENT '驳回类型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_version_reviews_psu_id (psu_id),
    INDEX idx_version_reviews_status (status),
    INDEX idx_version_reviews_psu_status (psu_id, status),
    INDEX idx_version_reviews_submitter (submitter_id),
    INDEX idx_version_reviews_reviewer (reviewer_id),
    INDEX idx_version_reviews_submitted (submitted_at),
    INDEX idx_version_reviews_git_hash (git_commit_hash),
    INDEX idx_version_reviews_comp_rev_status (composition_id, composition_revision_no, status),
    
    CONSTRAINT uk_version_reviews_version UNIQUE (psu_id, version_no)
) COMMENT 'AI Prompt版本审核表';

-- 创建审核发布记录表（与编辑流程解耦）
CREATE TABLE IF NOT EXISTS ai_prompt_release_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    psu_version_no INT NOT NULL COMMENT 'PSU版本号',
    json_schema_id BIGINT NOT NULL COMMENT 'Schema记录ID',
    json_schema_version_no INT NOT NULL COMMENT 'Schema版本号',
    prompt_id BIGINT NOT NULL COMMENT 'Prompt快照ID（composition_id）',
    prompt_version_no INT NOT NULL COMMENT 'Prompt版本号（composition_revision_no）',
    tag ENUM('PREVIEW', 'FORMAL') NOT NULL DEFAULT 'PREVIEW' COMMENT '发布标签',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_release_versions_psu_ver (psu_id, psu_version_no),
    INDEX idx_release_versions_psu_id (psu_id),
    INDEX idx_release_versions_tag (tag)
) COMMENT 'AI Prompt审核发布记录表';

-- 创建系统配置表
CREATE TABLE IF NOT EXISTS ai_prompt_system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值（加密存储）',
    config_type ENUM('API_KEY', 'OTHER') NOT NULL DEFAULT 'OTHER' COMMENT '配置类型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_system_configs_key (config_key),
    INDEX idx_system_configs_type (config_type)
) COMMENT 'AI Prompt系统配置表（仅存储API密钥等核心配置）';

-- 创建审计日志表
CREATE TABLE IF NOT EXISTS ai_prompt_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) NOT NULL COMMENT '操作用户名',
    operation VARCHAR(100) NOT NULL COMMENT '操作类型',
    targetType VARCHAR(50) NOT NULL COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    details JSON COMMENT '操作详情',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_audit_logs_user_id (user_id),
    INDEX idx_audit_logs_operation (operation),
    INDEX idx_audit_logs_target_type (targetType),
    INDEX idx_audit_logs_target_id (target_id),
    INDEX idx_audit_logs_created (created_at)
) COMMENT 'AI Prompt审计日志表';

-- 创建测试数据集表
CREATE TABLE IF NOT EXISTS ai_prompt_test_datasets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    name VARCHAR(200) NOT NULL COMMENT '数据集名称',
    data_content TEXT NOT NULL COMMENT '测试数据内容（JSON格式）',
    description VARCHAR(500) COMMENT '描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_test_datasets_psu_id (psu_id)
) COMMENT 'AI Prompt测试数据集表';

-- 创建编排草稿表
CREATE TABLE IF NOT EXISTS ai_prompt_compositions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    schema_version INT NOT NULL DEFAULT 1 COMMENT '绑定Schema版本',
    status ENUM('DRAFT', 'CANDIDATE', 'FORMAL', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT' COMMENT '编排状态：草稿/候选/正式/归档',
    content LONGTEXT COMMENT '编辑器原始内容',
    spec_json LONGTEXT COMMENT '编排规格JSON',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    rejection_reason VARCHAR(500) COMMENT '驳回原因',
    rejection_type VARCHAR(20) COMMENT '驳回类型: BACK_TO_DEV/BACK_TO_BIZ',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_compositions_psu_id (psu_id),
    INDEX idx_compositions_status (status),
    
    CONSTRAINT uk_compositions_psu_id UNIQUE (psu_id)
) COMMENT 'AI Prompt编排草稿表';

-- 创建编排版本快照表
CREATE TABLE IF NOT EXISTS ai_prompt_composition_revisions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    composition_id BIGINT NOT NULL COMMENT '关联编排ID',
    revision_no INT NOT NULL COMMENT '版本号',
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    status_at_time ENUM('DRAFT', 'CANDIDATE', 'FORMAL', 'ARCHIVED') NOT NULL COMMENT '快照状态',
    schema_version INT NOT NULL COMMENT 'Schema版本（兼容历史字段）',
    schema_version_at_time INT NOT NULL COMMENT 'Schema版本快照',
    content_snapshot LONGTEXT NOT NULL COMMENT '编排内容快照',
    spec_json_snapshot LONGTEXT NOT NULL COMMENT '编排规格快照',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_comp_rev (composition_id, revision_no),
    INDEX idx_comp_rev_psu_id (psu_id)
) COMMENT 'AI Prompt编排版本快照表';

-- 创建测试运行记录表
CREATE TABLE IF NOT EXISTS ai_prompt_test_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    dataset_id BIGINT NOT NULL COMMENT '关联测试集ID',
    composition_id BIGINT NOT NULL COMMENT '关联编排ID',
    composition_revision_no INT NULL COMMENT '关联快照版本',
    total_cases INT NOT NULL DEFAULT 0 COMMENT '总用例数',
    success_cases INT NOT NULL DEFAULT 0 COMMENT '成功用例数',
    failed_cases INT NOT NULL DEFAULT 0 COMMENT '失败用例数',
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING' COMMENT '测试状态（DB不校验，后端校验）',
    exception_reason TEXT NULL COMMENT '运行异常原因（DB不校验，后端校验）',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '执行人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_test_runs_psu_id (psu_id),
    INDEX idx_test_runs_dataset_id (dataset_id)
) COMMENT 'AI Prompt测试运行记录表';

-- 创建测试运行用例明细表
CREATE TABLE IF NOT EXISTS ai_prompt_test_run_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL COMMENT '关联运行记录ID',
    case_id VARCHAR(100) NOT NULL COMMENT '用例ID',
    case_name VARCHAR(200) NOT NULL COMMENT '用例名称',
    input_json LONGTEXT COMMENT '输入参数JSON',
    rendered_prompt LONGTEXT COMMENT '渲染后的Prompt',
    model_output LONGTEXT COMMENT '实际输出（兼容历史字段名）',
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '用例状态（DB不校验，后端校验）',
    error_message TEXT COMMENT '错误信息',
    exception_reason TEXT NULL COMMENT '用例异常原因（DB不校验，后端校验）',
    success TINYINT(1) NOT NULL COMMENT '是否成功',
    latency_ms INT NULL COMMENT '耗时(毫秒)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_test_run_items_run_id (run_id),
    INDEX idx_test_run_items_case_id (case_id)
) COMMENT 'AI Prompt测试运行用例明细表';

-- 创建发布单表
CREATE TABLE IF NOT EXISTS ai_prompt_releases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    environment VARCHAR(32) NOT NULL COMMENT '环境: DEV/STAGING/PROD',
    release_type ENUM('FULL', 'CANARY') NOT NULL COMMENT '发布类型',
    target_composition_id BIGINT NOT NULL COMMENT '目标编排ID',
    target_revision_no INT NOT NULL COMMENT '目标快照版本号',
    base_revision_no INT NULL COMMENT '灰度基线快照版本号',
    status ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'RELEASING', 'SUCCESS', 'FAILED', 'ROLLED_BACK', 'CANCELLED') NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态',
    approval_by BIGINT NULL COMMENT '审核人',
    approved_at DATETIME NULL COMMENT '审核时间',
    executed_by BIGINT NULL COMMENT '执行人',
    executed_at DATETIME NULL COMMENT '执行时间',
    rollback_to_revision_no INT NULL COMMENT '回滚目标快照版本号',
    rollback_reason VARCHAR(500) NULL COMMENT '回滚原因',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_release_psu_env_status (psu_id, environment, status),
    INDEX idx_release_target (target_composition_id, target_revision_no)
) COMMENT 'AI Prompt发布单表';

-- 创建发布规则表
CREATE TABLE IF NOT EXISTS ai_prompt_release_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    release_id BIGINT NOT NULL COMMENT '关联发布单ID',
    rule_type ENUM('WHITELIST', 'TAG', 'PERCENT') NOT NULL COMMENT '规则类型',
    rule_key VARCHAR(64) NULL COMMENT '匹配字段，如tenantId/channel/userId',
    operator VARCHAR(16) NULL COMMENT '操作符: EQ/IN/REGEX/RANGE',
    rule_value TEXT NULL COMMENT '规则值',
    traffic_percent INT NULL COMMENT '流量百分比，仅PERCENT有效',
    priority INT NOT NULL DEFAULT 100 COMMENT '优先级，越小越先匹配',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_rules_release (release_id, enabled, priority)
) COMMENT 'AI Prompt发布规则表';

-- 创建环境生效指针表
CREATE TABLE IF NOT EXISTS ai_prompt_live_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    environment VARCHAR(32) NOT NULL COMMENT '环境: DEV/STAGING/PROD',
    stable_release_id BIGINT NOT NULL COMMENT '稳定发布单ID',
    stable_revision_no INT NOT NULL COMMENT '稳定快照版本号',
    canary_release_id BIGINT NULL COMMENT '灰度发布单ID',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_live_psu_env (psu_id, environment)
) COMMENT 'AI Prompt环境生效版本指针表';

-- 创建回滚记录表
CREATE TABLE IF NOT EXISTS ai_prompt_rollbacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    environment VARCHAR(32) NOT NULL COMMENT '环境: DEV/STAGING/PROD',
    from_release_id BIGINT NOT NULL COMMENT '回滚前发布单ID',
    from_revision_no INT NOT NULL COMMENT '回滚前快照版本号',
    to_release_id BIGINT NOT NULL COMMENT '回滚后发布单ID',
    to_revision_no INT NOT NULL COMMENT '回滚后快照版本号',
    reason VARCHAR(500) NULL COMMENT '回滚原因',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_rollbacks_psu_env (psu_id, environment),
    INDEX idx_rollbacks_created_at (created_at)
) COMMENT 'AI Prompt回滚记录表';

-- 创建评估任务表
CREATE TABLE IF NOT EXISTS ai_prompt_evaluation_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
    dataset_id BIGINT NOT NULL COMMENT '关联测试集ID',
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '任务状态（后端校验）',
    total_cases INT NOT NULL DEFAULT 0 COMMENT '总用例数',
    processed_cases INT NOT NULL DEFAULT 0 COMMENT '已处理用例数',
    success_cases INT NOT NULL DEFAULT 0 COMMENT '成功用例数',
    failed_cases INT NOT NULL DEFAULT 0 COMMENT '失败用例数',
    average_score DECIMAL(5,2) NULL COMMENT '平均分',
    error_message TEXT NULL COMMENT '任务错误信息',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME NULL COMMENT '开始时间',
    finished_at DATETIME NULL COMMENT '结束时间',

    INDEX idx_eval_tasks_psu_id (psu_id),
    INDEX idx_eval_tasks_dataset_id (dataset_id),
    INDEX idx_eval_tasks_created_at (created_at)
) COMMENT 'AI Prompt评估任务表';

-- 创建评估明细结果表
CREATE TABLE IF NOT EXISTS ai_prompt_evaluation_item_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '关联评估任务ID',
    case_id VARCHAR(100) NOT NULL COMMENT '用例ID',
    case_name VARCHAR(200) NOT NULL COMMENT '用例名称',
    input_json LONGTEXT COMMENT '输入参数JSON',
    rendered_prompt LONGTEXT COMMENT '渲染Prompt',
    actual_output LONGTEXT COMMENT '实际输出',
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '用例状态（后端校验）',
    relevance_score DECIMAL(5,2) NULL COMMENT '相关性评分',
    completeness_score DECIMAL(5,2) NULL COMMENT '完整性评分',
    format_score DECIMAL(5,2) NULL COMMENT '格式符合度评分',
    total_score DECIMAL(5,2) NULL COMMENT '总分',
    reason TEXT NULL COMMENT '评分或失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_eval_items_task_id (task_id),
    INDEX idx_eval_items_case_id (case_id),
    INDEX idx_eval_items_created_at (created_at)
) COMMENT 'AI Prompt评估明细结果表';

-- 创建评估报告表
CREATE TABLE IF NOT EXISTS ai_prompt_evaluation_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '关联评估任务ID',
    overall_score DECIMAL(5,2) NULL COMMENT '总体评分',
    pass_rate DECIMAL(5,2) NULL COMMENT '通过率',
    summary_json LONGTEXT NULL COMMENT '报告摘要JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_eval_reports_task_id (task_id),
    INDEX idx_eval_reports_created_at (created_at)
) COMMENT 'AI Prompt评估报告表';

-- 初始化预置用户（每种角色一个）
INSERT IGNORE INTO ai_prompt_users (username, password, role, enabled) 
VALUES 
('admin_user', 'Admin@123', 'ADMIN', 1),
('dev_user', 'Dev@123', 'DEVELOPER', 1),
('bus_user', 'Bus@123', 'BUSINESS', 1);

INSERT IGNORE INTO ai_prompt_system_configs (config_key, config_value, config_type) 
VALUES ('default_api_key', 'encrypted_value_placeholder', 'API_KEY');
