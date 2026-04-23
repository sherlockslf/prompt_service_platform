package com.example.psu.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PsuUnit;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PsuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON Schema管理服务
 */
@Service
public class JsonSchemaService {
    
    @Autowired
    private JsonSchemaRepository jsonSchemaRepository;
    
    @Autowired
    private PsuRepository psuRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 获取Schema（研发可编辑，业务只读）
     * @param psuId PSU ID
     * @param userId 用户ID
     * @return Schema实体
     */
    public JsonSchema getSchemaByPsuId(Long psuId, Long userId) {
        // 检查PSU是否存在
        psuRepository.findById(psuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + psuId));

        return jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId)
                .orElseThrow(() -> new RuntimeException("Schema not found for PSU: " + psuId));
    }
    
    /**
     * 更新Schema（仅研发可操作）
     * @param psuId PSU ID
     * @param schemaContent Schema内容
     * @param userId 用户ID
     * @param changeLog 变更日志
     * @return 更新后的Schema
     */
    public JsonSchema updateSchema(Long psuId, String schemaContent, Long userId, String changeLog) {
        // 检查PSU是否存在
        PsuUnit psu = psuRepository.findById(psuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + psuId));
        
        // 验证Schema格式
        try {
            objectMapper.readTree(schemaContent);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON Schema format");
        }
        
        // 获取当前最高版本号
        JsonSchema latestSchema = jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId).orElse(null);
        int newVersion = latestSchema != null ? latestSchema.getVersion() + 1 : 1;
        
        // 创建新的Schema版本
        JsonSchema newSchema = new JsonSchema();
        newSchema.setPsuId(psuId);
        newSchema.setSchemaContent(schemaContent);
        newSchema.setVersion(newVersion);
        newSchema.setModifiedBy(userId == null ? 0L : userId);
        newSchema.setChangeLog(changeLog);
        newSchema.setCreatedAt(LocalDateTime.now());
        
        JsonSchema savedSchema = jsonSchemaRepository.save(newSchema);
        
        // 更新PSU的版本号（次版本号递增）
        // 注意：这里需要注入VersionReviewService
        // 由于循环依赖问题，我们直接在这里更新PSU版本
        psu.setMinorVersion(psu.getMinorVersion() + 1);
        psu.setPatchVersion(0); // 重置修订版本号
        psuRepository.save(psu);
        
        return savedSchema;
    }
    
    /**
     * 获取Schema版本历史
     * @param psuId PSU ID
     * @return Schema版本列表
     */
    public List<JsonSchema> getSchemaVersions(Long psuId) {
        return jsonSchemaRepository.findByPsuIdOrderByVersionDesc(psuId);
    }
}
