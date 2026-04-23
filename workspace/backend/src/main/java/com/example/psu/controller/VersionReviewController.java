package com.example.psu.controller;

import com.example.psu.dto.request.ReviewRequest;
import com.example.psu.entity.VersionReview;
import com.example.psu.service.VersionReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 版本审核控制器
 */
@RestController
@RequestMapping("/api/versions")
public class VersionReviewController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    private final VersionReviewService versionReviewService;

    public VersionReviewController(VersionReviewService versionReviewService) {
        this.versionReviewService = versionReviewService;
    }

    /**
     * 版本审核页面分页查询审核列表
     * 参数：psuId(可选)-按PSU筛选，page-页码(默认1)，size-每页大小(默认10)
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
     * 编排审核快照页面获取特定审核记录详情
     * 参数：id-审核记录ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVersionReview(@PathVariable Long id) {
        return versionReviewService.getVersionReviewById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Version review not found")));
    }

    /**
     * 业务侧/开发侧提交版本进入审核流程
     * 参数：psuId-PSU数据库ID
     */
    @PostMapping("/{psuId}/submit")
    public ResponseEntity<VersionReview> submitVersion(@PathVariable Long psuId) {
        return ResponseEntity.ok(versionReviewService.submitVersion(psuId, DEFAULT_OPERATOR_ID));
    }

    /**
     * 开发侧版本审核页面执行审核操作（通过/驳回）
     * 参数：reviewId-审核记录ID，approved-是否通过，rejectionReason-驳回原因，rejectionType-驳回类型
     */
    @PostMapping("/{reviewId}/review")
    public ResponseEntity<VersionReview> reviewVersion(@PathVariable Long reviewId, @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(versionReviewService.reviewVersion(reviewId, request, DEFAULT_OPERATOR_ID));
    }

    /**
     * 开发侧版本审核页面/代码生成页面获取已审核通过的生成代码
     * 参数：psuId-PSU数据库ID
     */
    @GetMapping("/{psuId}/code")
    public ResponseEntity<String> getCode(@PathVariable Long psuId) {
        return ResponseEntity.ok(versionReviewService.getCode(psuId));
    }

    /**
     * 开发侧版本审核页面登记Git提交哈希
     * 参数：reviewId-审核记录ID，gitCommitHash-Git提交哈希值
     */
    @PostMapping("/{reviewId}/git-commit")
    public ResponseEntity<VersionReview> registerGitCommit(
        @PathVariable Long reviewId,
        @RequestBody Map<String, String> payload
    ) {
        String gitCommitHash = payload == null ? null : payload.get("gitCommitHash");
        return ResponseEntity.ok(versionReviewService.registerGitCommit(reviewId, gitCommitHash, DEFAULT_OPERATOR_ID));
    }
}
