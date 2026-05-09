package com.example.psu.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.example.psu.dto.request.UpdateSchemaRequest;
import com.example.psu.dto.response.JsonSchemaResponse;
import com.example.psu.entity.JsonSchema;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.UserRepository;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.JsonSchemaService;

import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;

/**
 * JSON Schema管理控制器
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供Schema查询、更新与历史版本接口
 */
@RestController
@RequestMapping("/api/schemas")
public class JsonSchemaController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;
    
    @Autowired
    private JsonSchemaService jsonSchemaService;
    
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AsyncDispatchService asyncDispatchService;
    
    /**
     * 查询指定 PSU 的当前 Schema。
     * 请求方法与路径：GET /api/schemas/by-psuId（兼容 /api/v1/...）。
     * 入参：psuId。
     * 返回：JsonSchemaResponse。
     */
    @GetMapping("/by-psuId")
    public ResponseEntity<JsonSchemaResponse> getSchemaByPsuId(@RequestParam Long psuId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        JsonSchema schema = jsonSchemaService.getSchemaByPsuId(psuId, DEFAULT_OPERATOR_ID);
        JsonSchemaResponse response = convertToResponse(schema);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新 Schema（同步）。
     * 请求方法与路径：PUT /api/schemas/by-psuId（兼容 /api/v1/...）。
     * 入参：psuId + UpdateSchemaRequest（baseVersionNo、schemaContent、changeLog）。
     * 返回：更新后的 JsonSchemaResponse。
     */
    @PostMapping("/by-psuId")
    public ResponseEntity<JsonSchemaResponse> updateSchema(
            @RequestParam Long psuId,
            @Valid @RequestBody UpdateSchemaRequest requestBody) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        requestBody = RequestValidationUtils.requireNonNull(requestBody, "requestBody");
        JsonSchema schema = jsonSchemaService.updateSchema(
            psuId,
            requestBody.getBaseVersionNo(),
            requestBody.getSchemaContent(),
            DEFAULT_OPERATOR_ID,
            requestBody.getChangeLog()
        );
        JsonSchemaResponse response = convertToResponse(schema);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新 Schema（异步）。
     * 请求方法与路径：PUT /api/schemas/by-psuId/async（兼容 /api/v1/...）。
     * 入参：psuId + UpdateSchemaRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-psuId/async")
    public ResponseEntity<String> updateSchemaAsync(
            @RequestParam Long psuId,
            @Valid @RequestBody UpdateSchemaRequest requestBody) {
        asyncDispatchService.dispatch(() -> jsonSchemaService.updateSchema(
            psuId,
            requestBody.getBaseVersionNo(),
            requestBody.getSchemaContent(),
            DEFAULT_OPERATOR_ID,
            requestBody.getChangeLog()
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 查询 Schema 历史版本列表。
     * 请求方法与路径：GET /api/schemas/by-psuId/versions（兼容 /api/v1/...）。
     * 入参：psuId。
     * 返回：JsonSchemaResponse 列表（按服务层默认顺序）。
     */
    @GetMapping("/by-psuId/versions")
    public ResponseEntity<List<JsonSchemaResponse>> getSchemaVersions(@RequestParam Long psuId) {
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
        // 覆盖写模式下仍统一补齐关键字段，便于前端历史抽屉稳定展示。
        if (response.getUpdatedAt() == null) {
            response.setUpdatedAt(safeSchema.getCreatedAt());
        }
        if (response.getChangeLog() == null) {
            response.setChangeLog("");
        }
        
        // 获取修改者名称
        Long modifierId = safeSchema.getModifiedBy() == null ? 0L : safeSchema.getModifiedBy();
        userRepository.findById(modifierId).ifPresent(modifier -> {
            response.setModifierName(modifier.getUsername());
        });
        
        return response;
    }
}





