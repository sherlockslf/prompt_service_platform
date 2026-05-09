-- 审核记录增加显式schema版本指针，统一“psuVersion -> schemaVersion/promptVersion”模型。

ALTER TABLE ai_prompt_version_reviews
  ADD COLUMN schema_version_no INT NULL COMMENT '审核版本指向的Schema版本号' AFTER code_content;

-- 优先按发布记录回填（最准确）
UPDATE ai_prompt_version_reviews vr
LEFT JOIN ai_prompt_release_versions rv
  ON rv.psu_id = vr.psu_id AND rv.psu_version_no = vr.version_no
SET vr.schema_version_no = rv.json_schema_version_no
WHERE vr.schema_version_no IS NULL;

-- 兜底按PSU历史版本回填
UPDATE ai_prompt_version_reviews vr
LEFT JOIN ai_prompt_psu p
  ON p.id = vr.psu_id
LEFT JOIN ai_prompt_psu_versions pv
  ON pv.psu_id = p.psu_id AND pv.version_no = vr.version_no
SET vr.schema_version_no = pv.schema_version_no
WHERE vr.schema_version_no IS NULL;

