-- 为PSU版本历史增加schema/prompt快照指针，支持历史版本提交审核。

ALTER TABLE ai_prompt_psu_versions
  ADD COLUMN schema_version_no INT NULL COMMENT '该版本关联的Schema版本号' AFTER change_source,
  ADD COLUMN composition_id BIGINT NULL COMMENT '该版本关联的编排ID' AFTER schema_version_no,
  ADD COLUMN composition_revision_no INT NULL COMMENT '该版本关联的编排快照版本号' AFTER composition_id;

-- 回填已有历史记录（近似回填：取当前可得最新schema与最新revision；后续新记录将实时准确写入）
UPDATE ai_prompt_psu_versions v
LEFT JOIN ai_prompt_json_schemas s
  ON s.psu_id = v.psu_ref_id
LEFT JOIN (
  SELECT c.psu_id, c.id AS composition_id
  FROM ai_prompt_compositions c
) c
  ON c.psu_id = v.psu_ref_id
LEFT JOIN (
  SELECT r.composition_id, MAX(r.revision_no) AS latest_revision_no
  FROM ai_prompt_composition_revisions r
  GROUP BY r.composition_id
) rr
  ON rr.composition_id = c.composition_id
SET
  v.schema_version_no = COALESCE(v.schema_version_no, (
    SELECT MAX(s2.version) FROM ai_prompt_json_schemas s2 WHERE s2.psu_id = v.psu_ref_id
  )),
  v.composition_id = COALESCE(v.composition_id, c.composition_id),
  v.composition_revision_no = COALESCE(v.composition_revision_no, rr.latest_revision_no)
WHERE v.schema_version_no IS NULL OR v.composition_id IS NULL OR v.composition_revision_no IS NULL;

