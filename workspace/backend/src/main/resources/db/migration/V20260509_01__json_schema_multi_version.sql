-- 将 ai_prompt_json_schemas 从“单PSU单行覆盖写”迁移为“单PSU多版本历史”。
-- 执行前请备份数据库。

-- 1) 删除旧唯一约束（psu_id 唯一）
ALTER TABLE ai_prompt_json_schemas
  DROP INDEX uk_json_schemas_psu_id;

-- 2) 增加新唯一约束（psu_id + version）
ALTER TABLE ai_prompt_json_schemas
  ADD CONSTRAINT uk_json_schemas_psu_version UNIQUE (psu_id, version);

-- 3) 补充索引（如不存在可忽略错误）
CREATE INDEX idx_json_schemas_version ON ai_prompt_json_schemas(version);

