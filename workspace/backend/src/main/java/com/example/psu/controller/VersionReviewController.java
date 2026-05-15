package com.example.psu.controller;

import com.example.psu.dto.request.ReviewRequest;
import com.example.psu.dto.request.RollbackVersionRequest;
import com.example.psu.dto.request.AssignVersionTagRequest;
import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.response.CompositionRenderResponse;
import com.example.psu.dto.response.VersionCompareResponse;
import com.example.psu.dto.response.VersionReviewSnapshotResponse;
import com.example.psu.entity.ParamSet;
import com.example.psu.entity.VersionReview;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.CompositionService;
import com.example.psu.service.ParamSetService;
import com.example.psu.service.VersionReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 版本审核控制器
 */
@RestController
@RequestMapping("/api/versions")
public class VersionReviewController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    private final VersionReviewService versionReviewService;
    private final CompositionService compositionService;
    private final ParamSetService paramSetService;
    private final ObjectMapper objectMapper;
    private final AsyncDispatchService asyncDispatchService;

    public VersionReviewController(
        VersionReviewService versionReviewService,
        CompositionService compositionService,
        ParamSetService paramSetService,
        ObjectMapper objectMapper,
        AsyncDispatchService asyncDispatchService
    ) {
        this.versionReviewService = versionReviewService;
        this.compositionService = compositionService;
        this.paramSetService = paramSetService;
        this.objectMapper = objectMapper;
        this.asyncDispatchService = asyncDispatchService;
    }

    /**
     * 分页查询版本审核列表。
     * 请求方法与路径：GET /api/versions（兼容 /api/v1/...）。
     * 入参：psuId（可选）、page、size。
     * 返回：Page<VersionReview>，按 submittedAt 倒序。
     */
    @GetMapping
    public ResponseEntity<Page<VersionReview>> getVersionReviews(
        @RequestParam(required = false) Long psuId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        return ResponseEntity.ok(versionReviewService.getVersionReviews(psuId, pageable));
    }

    /**
     * 查询单个版本审核详情。
     * 请求方法与路径：GET /api/versions/by-id（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：VersionReview 或标准错误体。
     */
    @GetMapping("/by-id")
    public ResponseEntity<?> getVersionReview(@RequestParam Long id) {
        return versionReviewService.getVersionReviewById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Version review not found")));
    }

    /**
     * 查询指定审核记录对应的编排快照明细。
     * 请求方法与路径：GET /api/versions/by-reviewId/snapshot（兼容 /api/v1/...）。
     * 入参：reviewId。
     * 返回：VersionReviewSnapshotResponse。
     */
    @GetMapping("/by-reviewId/snapshot")
    public ResponseEntity<VersionReviewSnapshotResponse> getReviewSnapshot(@RequestParam Long reviewId) {
        return ResponseEntity.ok(versionReviewService.getReviewSnapshot(reviewId));
    }

    /**
     * 提交版本进入审核流程（同步）。
     * 请求方法与路径：POST /api/versions/by-psuId/submit（兼容 /api/v1/...）。
     * 入参：psuId。
     * 返回：VersionReview（状态通常为 CANDIDATE）。
     */
    @PostMapping("/by-psuId/submit")
    public ResponseEntity<VersionReview> submitVersion(
        @RequestParam Long psuId,
        @RequestParam(required = false) Integer versionNo
    ) {
        if (versionNo == null) {
            return ResponseEntity.ok(versionReviewService.submitVersion(psuId, DEFAULT_OPERATOR_ID));
        }
        return ResponseEntity.ok(versionReviewService.submitVersion(psuId, versionNo, DEFAULT_OPERATOR_ID));
    }

    /**
     * 提交版本进入审核流程（异步）。
     * 请求方法与路径：POST /api/versions/by-psuId/submit/async（兼容 /api/v1/...）。
     * 入参：psuId。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-psuId/submit/async")
    public ResponseEntity<String> submitVersionAsync(
        @RequestParam Long psuId,
        @RequestParam(required = false) Integer versionNo
    ) {
        if (versionNo == null) {
            asyncDispatchService.dispatch(() -> versionReviewService.submitVersion(psuId, DEFAULT_OPERATOR_ID));
        } else {
            asyncDispatchService.dispatch(() -> versionReviewService.submitVersion(psuId, versionNo, DEFAULT_OPERATOR_ID));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 执行审核动作（通过/驳回，同步）。
     * 请求方法与路径：POST /api/versions/by-reviewId/review（兼容 /api/v1/...）。
     * 入参：reviewId + ReviewRequest。
     * 返回：更新后的 VersionReview。
     */
    @PostMapping("/by-reviewId/review")
    public ResponseEntity<VersionReview> reviewVersion(@RequestParam Long reviewId, @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(versionReviewService.reviewVersion(reviewId, request, DEFAULT_OPERATOR_ID));
    }

    /**
     * 执行审核动作（异步）。
     * 请求方法与路径：POST /api/versions/by-reviewId/review/async（兼容 /api/v1/...）。
     * 入参：reviewId + ReviewRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-reviewId/review/async")
    public ResponseEntity<String> reviewVersionAsync(@RequestParam Long reviewId, @RequestBody ReviewRequest request) {
        asyncDispatchService.dispatch(() -> versionReviewService.reviewVersion(reviewId, request, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 审核预览：按参数集渲染待审编排。
     * 请求方法与路径：GET /api/versions/by-reviewId/preview（兼容 /api/v1/...）。
     * 入参：reviewId。
     * 返回：CompositionRenderResponse（含 paramSetSnapshot）。
     */
    @GetMapping("/by-reviewId/preview")
    public ResponseEntity<CompositionRenderResponse> previewByParamSet(@RequestParam Long reviewId) {
        VersionReview review = versionReviewService.getVersionReviewById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("审核记录不存在: " + reviewId));
        ParamSet paramSet = paramSetService.getParamSetByPsuId(review.getPsuId());
        try {
            // 先解析参数集，再在响应中回传快照，便于前端审核定位输入上下文。
            Map<String, Object> paramSnapshot = objectMapper.readValue(
                paramSet.getParamSetContent(),
                new TypeReference<Map<String, Object>>() {}
            );
            CompositionRenderRequest request = new CompositionRenderRequest();
            request.setCompositionId(review.getCompositionId());
            request.setInput(paramSnapshot);
            CompositionRenderResponse response = compositionService.render(review.getPsuId(), request);
            response.setParamSetSnapshot(paramSnapshot);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new IllegalArgumentException("参数集解析失败，无法渲染预览");
        }
    }

    /**
     * 获取指定 PSU 的生成代码快照。
     * 请求方法与路径：GET /api/versions/by-psuId/code（兼容 /api/v1/...）。
     * 入参：psuId、language（java/python，默认 java）。
     * 返回：代码文本。
     */
    @GetMapping("/by-psuId/code")
    public ResponseEntity<String> getCode(
        @RequestParam Long psuId,
        @RequestParam(required = false) Integer versionNo,
        @RequestParam(required = false, defaultValue = "java") String language
    ) {
        return ResponseEntity.ok(versionReviewService.getCode(psuId, versionNo, language));
    }

    @GetMapping("/by-psuId/code/download")
    public ResponseEntity<byte[]> downloadCode(
        @RequestParam Long psuId,
        @RequestParam(required = false) Integer versionNo,
        @RequestParam(required = false, defaultValue = "java") String language
    ) {
        byte[] zipBytes = versionReviewService.downloadCodeBundle(psuId, versionNo, language);
        String fileName = "psu-" + psuId + (versionNo == null ? "" : "-v" + versionNo) + "-" + language + "-bundle.zip";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(zipBytes);
    }

    /**
     * 对比两个版本快照差异。
     * 请求方法与路径：GET /api/versions/by-psuId/compare（兼容 /api/v1/...）。
     * 入参：psuId、fromVersionNo、toVersionNo。
     * 返回：VersionCompareResponse。
     */
    @GetMapping("/by-psuId/compare")
    public ResponseEntity<VersionCompareResponse> compareVersions(
        @RequestParam Long psuId,
        @RequestParam Integer fromVersionNo,
        @RequestParam Integer toVersionNo
    ) {
        return ResponseEntity.ok(versionReviewService.compareVersions(psuId, fromVersionNo, toVersionNo));
    }

    /**
     * 版本回滚（同步）。
     * 请求方法与路径：POST /api/versions/by-psuId/rollback（兼容 /api/v1/...）。
     * 入参：psuId + RollbackVersionRequest。
     * 返回：回滚后的 VersionReview。
     */
    @PostMapping("/by-psuId/rollback")
    public ResponseEntity<VersionReview> rollbackVersion(
        @RequestParam Long psuId,
        @RequestBody RollbackVersionRequest request
    ) {
        return ResponseEntity.ok(versionReviewService.rollbackVersion(psuId, request, DEFAULT_OPERATOR_ID));
    }

    /**
     * 版本回滚（异步）。
     * 请求方法与路径：POST /api/versions/by-psuId/rollback/async（兼容 /api/v1/...）。
     * 入参：psuId + RollbackVersionRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-psuId/rollback/async")
    public ResponseEntity<String> rollbackVersionAsync(
        @RequestParam Long psuId,
        @RequestBody RollbackVersionRequest request
    ) {
        asyncDispatchService.dispatch(() -> versionReviewService.rollbackVersion(psuId, request, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 登记版本关联的 Git Commit（同步）。
     * 请求方法与路径：POST /api/versions/by-reviewId/git-commit（兼容 /api/v1/...）。
     * 入参：reviewId + payload.gitCommitHash。
     * 返回：更新后的 VersionReview。
     */
    @PostMapping("/by-reviewId/git-commit")
    public ResponseEntity<VersionReview> registerGitCommit(
        @RequestParam Long reviewId,
        @RequestBody Map<String, String> payload
    ) {
        String gitCommitHash = payload == null ? null : payload.get("gitCommitHash");
        return ResponseEntity.ok(versionReviewService.registerGitCommit(reviewId, gitCommitHash, DEFAULT_OPERATOR_ID));
    }

    /**
     * 登记版本关联的 Git Commit（异步）。
     * 请求方法与路径：POST /api/versions/by-reviewId/git-commit/async（兼容 /api/v1/...）。
     * 入参：reviewId + payload.gitCommitHash。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-reviewId/git-commit/async")
    public ResponseEntity<String> registerGitCommitAsync(
        @RequestParam Long reviewId,
        @RequestBody Map<String, String> payload
    ) {
        String gitCommitHash = payload == null ? null : payload.get("gitCommitHash");
        asyncDispatchService.dispatch(() -> versionReviewService.registerGitCommit(reviewId, gitCommitHash, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 手动指定版本标签（同步）。
     * 请求方法与路径：POST /api/versions/by-reviewId/tag（兼容 /api/v1/...）。
     * 入参：reviewId + AssignVersionTagRequest（FORMAL/PREVIEW）。
     * 返回：更新后的 VersionReview。
     */
    @PostMapping("/by-reviewId/tag")
    public ResponseEntity<VersionReview> assignVersionTag(
        @RequestParam Long reviewId,
        @RequestBody AssignVersionTagRequest request
    ) {
        if (request == null || request.getTag() == null) {
            throw new IllegalArgumentException("tag不能为空");
        }
        return ResponseEntity.ok(versionReviewService.assignVersionTag(reviewId, request.getTag(), DEFAULT_OPERATOR_ID));
    }

    /**
     * 手动指定版本标签（异步）。
     * 请求方法与路径：POST /api/versions/by-reviewId/tag/async（兼容 /api/v1/...）。
     * 入参：reviewId + AssignVersionTagRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-reviewId/tag/async")
    public ResponseEntity<String> assignVersionTagAsync(
        @RequestParam Long reviewId,
        @RequestBody AssignVersionTagRequest request
    ) {
        if (request == null || request.getTag() == null) {
            throw new IllegalArgumentException("tag不能为空");
        }
        asyncDispatchService.dispatch(() -> versionReviewService.assignVersionTag(reviewId, request.getTag(), DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
}




