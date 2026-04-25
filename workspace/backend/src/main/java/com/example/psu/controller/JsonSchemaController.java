package com.example.psu.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.example.psu.dto.request.UpdateSchemaRequest;
import com.example.psu.dto.response.JsonSchemaResponse;
import com.example.psu.entity.JsonSchema;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.UserRepository;
import com.example.psu.service.JsonSchemaService;

import jakarta.validation.Valid;
import java.util.Objects;

/**
 * JSON Schema管理控制器
 */
@RestController
@RequestMapping("/api/schemas")
public class JsonSchemaController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;
    
    @Autowired
    private JsonSchemaService jsonSchemaService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 开发侧/业务侧Schema编辑器页面获取指定PSU的Schema
     * 参数：psuId-PSU数据库ID
     */
    @GetMapping("/{psuId}")
    public ResponseEntity<JsonSchemaResponse> getSchemaByPsuId(@PathVariable Long psuId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        JsonSchema schema = jsonSchemaService.getSchemaByPsuId(psuId, DEFAULT_OPERATOR_ID);
        JsonSchemaResponse response = convertToResponse(schema);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 开发侧Schema编辑器页面更新Schema内容
     * 参数：psuId-PSU数据库ID，schemaContent-Schema JSON内容，changeLog-变更日志
     */
    @PutMapping("/{psuId}")
    public ResponseEntity<JsonSchemaResponse> updateSchema(
            @PathVariable Long psuId,
            @Valid @RequestBody UpdateSchemaRequest requestBody) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        requestBody = RequestValidationUtils.requireNonNull(requestBody, "requestBody");
        JsonSchema schema = jsonSchemaService.updateSchema(psuId, requestBody.getSchemaContent(), DEFAULT_OPERATOR_ID, requestBody.getChangeLog());
        JsonSchemaResponse response = convertToResponse(schema);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 开发侧Schema编辑器页面查看Schema版本历史
     * 参数：psuId-PSU数据库ID
     */
    @GetMapping("/{psuId}/versions")
    public ResponseEntity<List<JsonSchemaResponse>> getSchemaVersions(@PathVariable Long psuId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        List<JsonSchema> versions = jsonSchemaService.getSchemaVersions(psuId);
        List<JsonSchemaResponse> responses = versions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 将Schema实体转换为响应DTO
     */
    private JsonSchemaResponse convertToResponse(JsonSchema schema) {
        RequestValidationUtils.requireNonNull(schema, "schema");
        JsonSchema safeSchema = Objects.requireNonNull(schema);
        JsonSchemaResponse response = new JsonSchemaResponse();
        BeanUtils.copyProperties(safeSchema, response);
        
        // 获取修改者名称
        Long modifierId = safeSchema.getModifiedBy() == null ? 0L : safeSchema.getModifiedBy();
        userRepository.findById(modifierId).ifPresent(modifier -> {
            response.setModifierName(modifier.getUsername());
        });
        
        return response;
    }
}


