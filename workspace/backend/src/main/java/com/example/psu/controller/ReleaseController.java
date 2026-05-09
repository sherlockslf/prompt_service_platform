package com.example.psu.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.psu.dto.request.CreateReleaseRequest;
import com.example.psu.dto.request.ReleaseRuleRequest;
import com.example.psu.dto.request.ResolvePromptRequest;
import com.example.psu.dto.response.PromptSchemaResolveResponse;
import com.example.psu.dto.request.ReviewReleaseRequest;
import com.example.psu.dto.request.RollbackReleaseRequest;
import com.example.psu.dto.response.ResolvePromptResponse;
import com.example.psu.enums.PsuTag;
import com.example.psu.entity.PromptRelease;
import com.example.psu.entity.PromptReleaseRule;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.ReleaseService;
import org.springframework.http.HttpStatus;

/**
 * 发布域控制器
 */
@RestController
public class ReleaseController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    private final ReleaseService releaseService;
    private final AsyncDispatchService asyncDispatchService;

    public ReleaseController(ReleaseService releaseService, AsyncDispatchService asyncDispatchService) {
        this.releaseService = releaseService;
        this.asyncDispatchService = asyncDispatchService;
    }

    /**
     * 分页查询发布单列表。
     * 请求方法与路径：GET /api/releases（兼容 /api/v1/releases）。
     * 入参：psuId、environment、page、size。
     * 返回：发布单分页结果，按更新时间倒序。
     */
    @GetMapping("/api/releases")
    public ResponseEntity<Page<PromptRelease>> getReleases(
        @RequestParam(required = false) Long psuId,
        @RequestParam(required = false) String environment,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ResponseEntity.ok(releaseService.getReleases(psuId, environment, pageable));
    }

    /**
     * 查询单个发布单详情。
     * 请求方法与路径：GET /api/releases/by-releaseId（兼容 /api/v1/...）。
     * 入参：releaseId。
     * 返回：PromptRelease。
     */
    @GetMapping("/api/releases/by-releaseId")
    public ResponseEntity<PromptRelease> getRelease(@RequestParam Long releaseId) {
        return ResponseEntity.ok(releaseService.getRelease(releaseId));
    }

    /**
     * 创建发布单（同步）。
     * 请求方法与路径：POST /api/releases（兼容 /api/v1/releases）。
     * 入参：CreateReleaseRequest。
     * 返回：创建后的发布单实体。
     */
    @PostMapping("/api/releases")
    public ResponseEntity<PromptRelease> createRelease(@RequestBody CreateReleaseRequest request) {
        return ResponseEntity.ok(releaseService.createRelease(request, DEFAULT_OPERATOR_ID));
    }

    /**
     * 创建发布单（异步）。
     * 请求方法与路径：POST /api/releases/async（兼容 /api/v1/...）。
     * 入参：CreateReleaseRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/api/releases/async")
    public ResponseEntity<String> createReleaseAsync(@RequestBody CreateReleaseRequest request) {
        asyncDispatchService.dispatch(() -> releaseService.createRelease(request, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 提交发布单进入待审批状态（同步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/submit（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/submit")
    public ResponseEntity<PromptRelease> submit(@RequestParam Long releaseId) {
        return ResponseEntity.ok(releaseService.submit(releaseId, DEFAULT_OPERATOR_ID));
    }
    /**
     * 提交发布单（异步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/submit/async（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/submit/async")
    public ResponseEntity<String> submitAsync(@RequestParam Long releaseId) {
        asyncDispatchService.dispatch(() -> releaseService.submit(releaseId, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 审批通过发布单（同步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/approve（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/approve")
    public ResponseEntity<PromptRelease> approve(@RequestParam Long releaseId) {
        return ResponseEntity.ok(releaseService.approve(releaseId, DEFAULT_OPERATOR_ID));
    }
    /**
     * 审批通过发布单（异步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/approve/async（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/approve/async")
    public ResponseEntity<String> approveAsync(@RequestParam Long releaseId) {
        asyncDispatchService.dispatch(() -> releaseService.approve(releaseId, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 驳回发布单（同步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/reject（兼容 /api/v1/...）。
     * 入参：ReviewReleaseRequest（可包含驳回原因）。
     */
    @PostMapping("/api/releases/by-releaseId/reject")
    public ResponseEntity<PromptRelease> reject(
        @RequestParam Long releaseId,
        @RequestBody(required = false) ReviewReleaseRequest request
    ) {
        return ResponseEntity.ok(releaseService.reject(releaseId, request, DEFAULT_OPERATOR_ID));
    }
    /**
     * 驳回发布单（异步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/reject/async（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/reject/async")
    public ResponseEntity<String> rejectAsync(
        @RequestParam Long releaseId,
        @RequestBody(required = false) ReviewReleaseRequest request
    ) {
        asyncDispatchService.dispatch(() -> releaseService.reject(releaseId, request, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 执行发布单（同步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/execute（兼容 /api/v1/...）。
     * 说明：执行成功后会推进在线版本指针。
     */
    @PostMapping("/api/releases/by-releaseId/execute")
    public ResponseEntity<PromptRelease> execute(@RequestParam Long releaseId) {
        return ResponseEntity.ok(releaseService.execute(releaseId, DEFAULT_OPERATOR_ID));
    }
    /**
     * 执行发布单（异步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/execute/async（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/execute/async")
    public ResponseEntity<String> executeAsync(@RequestParam Long releaseId) {
        asyncDispatchService.dispatch(() -> releaseService.execute(releaseId, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 回滚发布单（同步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/rollback（兼容 /api/v1/...）。
     * 入参：RollbackReleaseRequest（回滚目标与说明）。
     */
    @PostMapping("/api/releases/by-releaseId/rollback")
    public ResponseEntity<PromptRelease> rollback(
        @RequestParam Long releaseId,
        @RequestBody RollbackReleaseRequest request
    ) {
        return ResponseEntity.ok(releaseService.rollback(releaseId, request, DEFAULT_OPERATOR_ID));
    }
    /**
     * 回滚发布单（异步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/rollback/async（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/rollback/async")
    public ResponseEntity<String> rollbackAsync(
        @RequestParam Long releaseId,
        @RequestBody RollbackReleaseRequest request
    ) {
        asyncDispatchService.dispatch(() -> releaseService.rollback(releaseId, request, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 查询发布规则列表。
     * 请求方法与路径：GET /api/releases/by-releaseId/rules（兼容 /api/v1/...）。
     */
    @GetMapping("/api/releases/by-releaseId/rules")
    public ResponseEntity<List<PromptReleaseRule>> getRules(@RequestParam Long releaseId) {
        return ResponseEntity.ok(releaseService.getRules(releaseId));
    }

    /**
     * 新增发布规则（同步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/rules（兼容 /api/v1/...）。
     * 入参：ReleaseRuleRequest。
     */
    @PostMapping("/api/releases/by-releaseId/rules")
    public ResponseEntity<PromptReleaseRule> addRule(
        @RequestParam Long releaseId,
        @RequestBody ReleaseRuleRequest request
    ) {
        return ResponseEntity.ok(releaseService.addRule(releaseId, request));
    }
    /**
     * 新增发布规则（异步）。
     * 请求方法与路径：POST /api/releases/by-releaseId/rules/async（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/rules/async")
    public ResponseEntity<String> addRuleAsync(
        @RequestParam Long releaseId,
        @RequestBody ReleaseRuleRequest request
    ) {
        asyncDispatchService.dispatch(() -> releaseService.addRule(releaseId, request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 更新发布规则（同步）。
     * 请求方法与路径：PUT /api/releases/by-releaseId/rules/by-ruleId（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/rules/by-ruleId")
    public ResponseEntity<PromptReleaseRule> updateRule(
        @RequestParam Long releaseId,
        @RequestParam Long ruleId,
        @RequestBody ReleaseRuleRequest request
    ) {
        return ResponseEntity.ok(releaseService.updateRule(releaseId, ruleId, request));
    }
    /**
     * 更新发布规则（异步）。
     * 请求方法与路径：PUT /api/releases/by-releaseId/rules/by-ruleId/async（兼容 /api/v1/...）。
     */
    @PostMapping("/api/releases/by-releaseId/rules/by-ruleId/async")
    public ResponseEntity<String> updateRuleAsync(
        @RequestParam Long releaseId,
        @RequestParam Long ruleId,
        @RequestBody ReleaseRuleRequest request
    ) {
        asyncDispatchService.dispatch(() -> releaseService.updateRule(releaseId, ruleId, request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 删除发布规则（同步）。
     * 请求方法与路径：DELETE /api/releases/by-releaseId/rules/by-ruleId（兼容 /api/v1/...）。
     */
    @DeleteMapping("/api/releases/by-releaseId/rules/by-ruleId")
    public ResponseEntity<Void> deleteRule(@RequestParam Long releaseId, @RequestParam Long ruleId) {
        releaseService.deleteRule(releaseId, ruleId);
        return ResponseEntity.ok().build();
    }
    /**
     * 删除发布规则（异步）。
     * 请求方法与路径：DELETE /api/releases/by-releaseId/rules/by-ruleId/async（兼容 /api/v1/...）。
     */
    @DeleteMapping("/api/releases/by-releaseId/rules/by-ruleId/async")
    public ResponseEntity<String> deleteRuleAsync(@RequestParam Long releaseId, @RequestParam Long ruleId) {
        asyncDispatchService.dispatch(() -> releaseService.deleteRule(releaseId, ruleId));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 解析线上生效提示词路由（同步）。
     * 请求方法与路径：POST /api/prompt-service/resolve（兼容 /api/v1/...）。
     * 入参：ResolvePromptRequest（psuId、环境、上下文）。
     * 返回：ResolvePromptResponse（路由类型、版本号、渲染配置）。
     */
    @PostMapping("/api/prompt-service/resolve")
    public ResponseEntity<ResolvePromptResponse> resolve(@RequestBody ResolvePromptRequest request) {
        return ResponseEntity.ok(releaseService.resolve(request));
    }
    /**
     * 解析线上生效提示词路由（异步）。
     * 请求方法与路径：POST /api/prompt-service/resolve/async（兼容 /api/v1/...）。
     */
    @PostMapping("/api/prompt-service/resolve/async")
    public ResponseEntity<String> resolveAsync(@RequestBody ResolvePromptRequest request) {
        asyncDispatchService.dispatch(() -> releaseService.resolve(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 查询指定PSU与标签对应的Prompt与Schema内容。
     * 请求方法与路径：GET /api/prompt-service/content（兼容 /api/v1/...）。
     * 入参：psuId（业务PSU标识）、tag（FORMAL/PREVIEW）。
     * 返回：PromptSchemaResolveResponse。
     */
    @GetMapping("/api/prompt-service/content")
    public ResponseEntity<PromptSchemaResolveResponse> getPromptAndSchema(
        @RequestParam String psuId,
        @RequestParam String tag
    ) {
        return ResponseEntity.ok(releaseService.getPromptAndSchema(psuId, PsuTag.valueOf(tag.trim().toUpperCase())));
    }
}


