package com.example.psu.controller;

import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.request.CompositionSaveRequest;
import com.example.psu.dto.response.CompositionRenderResponse;
import com.example.psu.dto.response.CompositionResponse;
import com.example.psu.dto.response.CompositionValidateResponse;
import com.example.psu.entity.PromptComposition;
import com.example.psu.service.CompositionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * 编排控制器
 */
@RestController
@RequestMapping("/api/compositions")
public class CompositionController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    private final CompositionService compositionService;
    private final ObjectMapper objectMapper;

    public CompositionController(
        CompositionService compositionService,
        ObjectMapper objectMapper
    ) {
        this.compositionService = compositionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 业务侧动态容器编排页面获取指定PSU的编排内容
     * 参数：psuId-PSU数据库ID
     */
    @GetMapping
    public ResponseEntity<?> getComposition(@RequestParam Long psuId) {
        Optional<PromptComposition> compositionOpt = compositionService.getCompositionByPsuId(psuId);
        if (compositionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Composition not found"));
        }
        return ResponseEntity.ok(toResponse(compositionOpt.get()));
    }

    /**
     * 业务侧动态容器编排页面保存编排草稿
     * 参数：psuId-PSU数据库ID，content-编排JSON内容，specJson-规格配置
     */
    @PutMapping
    public ResponseEntity<?> saveDraft(@RequestParam Long psuId, @RequestBody CompositionSaveRequest request) {
        PromptComposition saved = compositionService.saveDraft(psuId, request, DEFAULT_OPERATOR_ID);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * 业务侧动态容器编排页面校验编排内容合法性
     * 参数：psuId-PSU数据库ID，content-待校验的编排JSON
     */
    @PostMapping("/validate")
    public ResponseEntity<CompositionValidateResponse> validate(@RequestParam Long psuId, @RequestBody CompositionSaveRequest request) {
        return ResponseEntity.ok(compositionService.validate(psuId, request));
    }

    /**
     * 业务侧动态容器编排页面渲染编排内容（替换变量占位符）
     * 参数：psuId-PSU数据库ID，variables-变量键值对
     */
    @PostMapping("/render")
    public ResponseEntity<CompositionRenderResponse> render(@RequestParam Long psuId, @RequestBody CompositionRenderRequest request) {
        return ResponseEntity.ok(compositionService.render(psuId, request));
    }

    /**
     * 业务侧动态容器编排页面提交编排进入审核流程
     * 参数：psuId-PSU数据库ID
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestParam Long psuId) {
        PromptComposition submitted = compositionService.submit(psuId, DEFAULT_OPERATOR_ID);
        return ResponseEntity.ok(toResponse(submitted));
    }

    /**
     * 编排审核快照页面获取最新编排快照版本
     * 参数：compositionId-编排数据库ID
     */
    @GetMapping("/{compositionId}/revisions/latest")
    public ResponseEntity<?> getLatestRevision(@PathVariable Long compositionId) {
        return compositionService.getLatestRevision(compositionId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Revision not found")));
    }

    private CompositionResponse toResponse(PromptComposition composition) {
        CompositionResponse response = new CompositionResponse();
        response.setId(composition.getId());
        response.setPsuId(composition.getPsuId());
        response.setStatus(composition.getStatus());
        response.setSchemaVersion(composition.getSchemaVersion());
        response.setContent(composition.getContent());
        response.setRejectionReason(composition.getRejectionReason());
        response.setRejectionType(composition.getRejectionType());
        response.setUpdatedAt(composition.getUpdatedAt());

        if (composition.getSpecJson() != null && !composition.getSpecJson().isBlank()) {
            try {
                Map<String, Object> spec = objectMapper.readValue(composition.getSpecJson(), new TypeReference<Map<String, Object>>() {
                });
                response.setSpecJson(spec);
            } catch (Exception ignored) {
                response.setSpecJson(Map.of());
            }
        }
        return response;
    }
}
