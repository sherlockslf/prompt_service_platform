package com.example.psu.controller;

import com.example.psu.dto.request.CreateReleaseRequest;
import com.example.psu.dto.request.ReleaseRuleRequest;
import com.example.psu.dto.request.ResolvePromptRequest;
import com.example.psu.dto.request.ReviewReleaseRequest;
import com.example.psu.dto.request.RollbackReleaseRequest;
import com.example.psu.dto.response.ResolvePromptResponse;
import com.example.psu.entity.PromptRelease;
import com.example.psu.entity.PromptReleaseRule;
import com.example.psu.service.ReleaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 发布域控制器
 */
@RestController
public class ReleaseController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

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

    @GetMapping("/api/releases/{releaseId}")
    public ResponseEntity<PromptRelease> getRelease(@PathVariable Long releaseId) {
        return ResponseEntity.ok(releaseService.getRelease(releaseId));
    }

    @PostMapping("/api/releases")
    public ResponseEntity<PromptRelease> createRelease(@RequestBody CreateReleaseRequest request) {
        return ResponseEntity.ok(releaseService.createRelease(request, DEFAULT_OPERATOR_ID));
    }

    @PostMapping("/api/releases/{releaseId}/submit")
    public ResponseEntity<PromptRelease> submit(@PathVariable Long releaseId) {
        return ResponseEntity.ok(releaseService.submit(releaseId, DEFAULT_OPERATOR_ID));
    }

    @PostMapping("/api/releases/{releaseId}/approve")
    public ResponseEntity<PromptRelease> approve(@PathVariable Long releaseId) {
        return ResponseEntity.ok(releaseService.approve(releaseId, DEFAULT_OPERATOR_ID));
    }

    @PostMapping("/api/releases/{releaseId}/reject")
    public ResponseEntity<PromptRelease> reject(
        @PathVariable Long releaseId,
        @RequestBody(required = false) ReviewReleaseRequest request
    ) {
        return ResponseEntity.ok(releaseService.reject(releaseId, request, DEFAULT_OPERATOR_ID));
    }

    @PostMapping("/api/releases/{releaseId}/execute")
    public ResponseEntity<PromptRelease> execute(@PathVariable Long releaseId) {
        return ResponseEntity.ok(releaseService.execute(releaseId, DEFAULT_OPERATOR_ID));
    }

    @PostMapping("/api/releases/{releaseId}/rollback")
    public ResponseEntity<PromptRelease> rollback(
        @PathVariable Long releaseId,
        @RequestBody RollbackReleaseRequest request
    ) {
        return ResponseEntity.ok(releaseService.rollback(releaseId, request, DEFAULT_OPERATOR_ID));
    }

    @GetMapping("/api/releases/{releaseId}/rules")
    public ResponseEntity<List<PromptReleaseRule>> getRules(@PathVariable Long releaseId) {
        return ResponseEntity.ok(releaseService.getRules(releaseId));
    }

    @PostMapping("/api/releases/{releaseId}/rules")
    public ResponseEntity<PromptReleaseRule> addRule(
        @PathVariable Long releaseId,
        @RequestBody ReleaseRuleRequest request
    ) {
        return ResponseEntity.ok(releaseService.addRule(releaseId, request));
    }

    @PutMapping("/api/releases/{releaseId}/rules/{ruleId}")
    public ResponseEntity<PromptReleaseRule> updateRule(
        @PathVariable Long releaseId,
        @PathVariable Long ruleId,
        @RequestBody ReleaseRuleRequest request
    ) {
        return ResponseEntity.ok(releaseService.updateRule(releaseId, ruleId, request));
    }

    @DeleteMapping("/api/releases/{releaseId}/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long releaseId, @PathVariable Long ruleId) {
        releaseService.deleteRule(releaseId, ruleId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/prompt-service/resolve")
    public ResponseEntity<ResolvePromptResponse> resolve(@RequestBody ResolvePromptRequest request) {
        return ResponseEntity.ok(releaseService.resolve(request));
    }
}
