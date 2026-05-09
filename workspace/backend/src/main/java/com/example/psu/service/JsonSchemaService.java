package com.example.psu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PsuUnit;
import com.example.psu.enums.PsuStatus;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PsuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON Schema管理服务
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供Schema覆盖写管理与版本历史查询能力
 */
@Service
public class JsonSchemaService {
    
    @Autowired
    private JsonSchemaRepository jsonSchemaRepository;
    
    @Autowired
    private PsuRepository psuRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PsuService psuService;
    
    /**
     * 获取Schema（研发可编辑，业务只读）
     * @param psuId PSU ID
     * @param userId 用户ID
     * @return Schema实体
     */
    public JsonSchema getSchemaByPsuId(Long psuId, Long userId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 检查PSU是否存在
        psuRepository.findById(safePsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));

        return jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(safePsuId)
                .orElseGet(() -> initializeEmptySchema(safePsuId, userId));
    }
    
    /**
     * 更新Schema（仅研发可操作）
     * @param psuId PSU ID
     * @param schemaContent Schema内容
     * @param userId 用户ID
     * @param changeLog 变更日志
     * @return 更新后的Schema
     */
    public JsonSchema updateSchema(Long psuId, Integer baseVersionNo, String schemaContent, Long userId, String changeLog) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonBlank(schemaContent, "schemaContent");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 检查PSU是否存在
        PsuUnit psu = psuRepository.findById(safePsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        if (psu.getStatus() == PsuStatus.ARCHIVED) {
            throw new RuntimeException("当前PSU已归档，不允许编辑Schema");
        }
        validateBaseVersion(psu, baseVersionNo);
        
        // 验证Schema格式
        try {
            objectMapper.readTree(schemaContent);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON Schema format");
        }
        
        // 多版本语义：每次更新都新增一条Schema版本记录，保留历史可追溯。
        int nextSchemaVersion = jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(safePsuId)
            .map(JsonSchema::getVersion)
            .orElse(0) + 1;
        JsonSchema schema = new JsonSchema();
        schema.setPsuId(safePsuId);
        schema.setSchemaContent(schemaContent);
        schema.setVersion(nextSchemaVersion);
        schema.setModifiedBy(userId == null ? 0L : userId);
        schema.setChangeLog(changeLog);

        JsonSchema savedSchema = jsonSchemaRepository.save(schema);
        
        // 编辑Schema：PSU版本号独立递增，并记录PSU历史快照。
        psuService.bumpVersionAndSnapshot(safePsuId, userId, "UPDATE_SCHEMA");
        
        return savedSchema;
    }
    
    /**
     * 获取Schema版本历史
     * @param psuId PSU ID
     * @return Schema版本列表
     */
    public List<JsonSchema> getSchemaVersions(Long psuId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 先校验PSU存在，避免前端误传时返回空列表造成误判。
        psuRepository.findById(safePsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        // 多版本模式：返回历史版本（新到旧）。
        return jsonSchemaRepository.findByPsuIdOrderByVersionDesc(safePsuId);
    }

    private JsonSchema initializeEmptySchema(Long psuId, Long userId) {
        JsonSchema schema = new JsonSchema();
        schema.setPsuId(psuId);
        schema.setSchemaContent("{}");
        schema.setVersion(1);
        schema.setModifiedBy(userId == null ? 0L : userId);
        schema.setChangeLog("初始化空Schema");
        return jsonSchemaRepository.save(schema);
    }

    private void validateBaseVersion(PsuUnit psu, Integer baseVersionNo) {
        if (baseVersionNo == null) {
            throw new RuntimeException("baseVersionNo不能为空");
        }
        int current = psu.getVersionNo() == null ? 0 : psu.getVersionNo();
        if (baseVersionNo > current) {
            throw new RuntimeException("baseVersionNo不能大于当前版本");
        }
        if (current - baseVersionNo > 1) {
            throw new RuntimeException("版本已发生并发更新，请刷新后重试");
        }
    }
}


