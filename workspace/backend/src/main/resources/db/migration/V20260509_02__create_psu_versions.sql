-- 新增PSU版本历史表，并将当前PSU主表快照回填为首批历史版本。

CREATE TABLE IF NOT EXISTS ai_prompt_psu_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psu_ref_id BIGINT NOT NULL COMMENT '关联当前PSU记录ID',
    psu_id VARCHAR(100) NOT NULL COMMENT '业务PSU ID',
    version_no INT NOT NULL COMMENT 'PSU版本号',
    name VARCHAR(200) NOT NULL COMMENT 'PSU名称快照',
    description TEXT COMMENT 'PSU描述快照',
    status ENUM('DRAFT', 'CANDIDATE', 'FORMAL', 'ARCHIVED') NOT NULL COMMENT '状态快照',
    operator_id BIGINT NOT NULL DEFAULT 0 COMMENT '操作人ID',
    change_source VARCHAR(64) NOT NULL COMMENT '变更来源',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_psu_versions_psu_version (psu_id, version_no),
    INDEX idx_psu_versions_psu_id (psu_id),
    INDEX idx_psu_versions_version_no (version_no),
    INDEX idx_psu_versions_created_at (created_at)
);

INSERT IGNORE INTO ai_prompt_psu_versions (
    psu_ref_id, psu_id, version_no, name, description, status, operator_id, change_source, created_at
)
SELECT
    id, psu_id, version_no, name, description, status, COALESCE(creator_id, 0), 'INIT_BACKFILL', COALESCE(updated_at, created_at, NOW())
FROM ai_prompt_psu;

