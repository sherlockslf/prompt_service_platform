package com.example.psu.controller;

import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.request.CompositionSaveRequest;
import com.example.psu.dto.response.CompositionRenderResponse;
import com.example.psu.dto.response.CompositionResponse;
import com.example.psu.dto.response.CompositionValidateResponse;
import com.example.psu.entity.PromptComposition;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.CompositionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final AsyncDispatchService asyncDispatchService;

    public CompositionController(
        CompositionService compositionService,
        ObjectMapper objectMapper,
        AsyncDispatchService asyncDispatchService
    ) {
        this.compositionService = compositionService;
        this.objectMapper = objectMapper;
        this.asyncDispatchService = asyncDispatchService;
    }

    /**
     * 查询指定 PSU 的编排草稿/当前编排。
     * 请求方法与路径：GET /api/compositions（兼容 /api/v1/...）。
     * 入参：psuId（query 参数）。
     * 返回：CompositionResponse；不存在时返回 404 错误体。
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
     * 保存编排草稿（同步）。
     * 请求方法与路径：PUT /api/compositions（兼容 /api/v1/...）。
     * 入参：psuId（query）+ CompositionSaveRequest。
     * 返回：保存后的 CompositionResponse。
     */
    @PutMapping
    public ResponseEntity<?> saveDraft(@RequestParam Long psuId, @RequestBody CompositionSaveRequest request) {
        PromptComposition saved = compositionService.saveDraft(psuId, request, DEFAULT_OPERATOR_ID);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * 保存编排草稿（异步）。
     * 请求方法与路径：PUT /api/compositions/async（兼容 /api/v1/...）。
     * 入参：psuId（query）+ CompositionSaveRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/async")
    public ResponseEntity<String> saveDraftAsync(@RequestParam Long psuId, @RequestBody CompositionSaveRequest request) {
        asyncDispatchService.dispatch(() -> compositionService.saveDraft(psuId, request, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 校验编排内容（同步）。
     * 请求方法与路径：POST /api/compositions/validate（兼容 /api/v1/...）。
     * 入参：psuId（query）+ CompositionSaveRequest。
     * 返回：CompositionValidateResponse。
     */
    @PostMapping("/validate")
    public ResponseEntity<CompositionValidateResponse> validate(@RequestParam Long psuId, @RequestBody CompositionSaveRequest request) {
        return ResponseEntity.ok(compositionService.validate(psuId, request));
    }

    /**
     * 校验编排内容（异步）。
     * 请求方法与路径：POST /api/compositions/validate/async（兼容 /api/v1/...）。
     * 入参：psuId（query）+ CompositionSaveRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/validate/async")
    public ResponseEntity<String> validateAsync(@RequestParam Long psuId, @RequestBody CompositionSaveRequest request) {
        asyncDispatchService.dispatch(() -> compositionService.validate(psuId, request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 渲染编排内容（同步）。
     * 请求方法与路径：POST /api/compositions/render（兼容 /api/v1/...）。
     * 入参：psuId（query）+ CompositionRenderRequest（input、compositionId 可选）。
     * 返回：CompositionRenderResponse（renderedPrompt、missingVars、usedVars）。
     */
    @PostMapping("/render")
    public ResponseEntity<CompositionRenderResponse> render(@RequestParam Long psuId, @RequestBody CompositionRenderRequest request) {
        return ResponseEntity.ok(compositionService.render(psuId, request));
    }

    /**
     * 渲染编排内容（异步）。
     * 请求方法与路径：POST /api/compositions/render/async（兼容 /api/v1/...）。
     * 入参：psuId（query）+ CompositionRenderRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/render/async")
    public ResponseEntity<String> renderAsync(@RequestParam Long psuId, @RequestBody CompositionRenderRequest request) {
        asyncDispatchService.dispatch(() -> compositionService.render(psuId, request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 提交编排进入审核流程（同步）。
     * 请求方法与路径：POST /api/compositions/submit（兼容 /api/v1/...）。
     * 入参：psuId（query）。
     * 返回：提交后的 CompositionResponse（状态通常为 CANDIDATE）。
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestParam Long psuId) {
        PromptComposition submitted = compositionService.submit(psuId, DEFAULT_OPERATOR_ID);
        return ResponseEntity.ok(toResponse(submitted));
    }

    /**
     * 提交编排进入审核流程（异步）。
     * 请求方法与路径：POST /api/compositions/submit/async（兼容 /api/v1/...）。
     * 入参：psuId（query）。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/submit/async")
    public ResponseEntity<String> submitAsync(@RequestParam Long psuId) {
        asyncDispatchService.dispatch(() -> compositionService.submit(psuId, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 查询编排最近一次快照。
     * 请求方法与路径：GET /api/compositions/by-compositionId/revisions/latest（兼容 /api/v1/...）。
     * 入参：compositionId。
     * 返回：PromptCompositionRevision；不存在时返回 404 错误体。
     */
    @GetMapping("/by-compositionId/revisions/latest")
    public ResponseEntity<?> getLatestRevision(@RequestParam Long compositionId) {
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




