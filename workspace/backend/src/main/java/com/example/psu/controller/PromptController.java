package com.example.psu.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.psu.dto.request.CreatePromptRequest;
import com.example.psu.dto.request.UpdatePromptRequest;
import com.example.psu.dto.response.PromptFragmentResponse;
import com.example.psu.dto.response.PromptTestResponse;
import com.example.psu.entity.PromptFragment;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.PromptService;

import jakarta.validation.Valid;
import java.util.Objects;

/**
 * Prompt管理控制器
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供Prompt片段管理与统一测试接口
 */
@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;
    
    @Autowired
    private PromptService promptService;
    @Autowired
    private AsyncDispatchService asyncDispatchService;
    
    /**
     * 查询指定 PSU 的 Prompt 片段列表。
     * 请求方法与路径：GET /api/prompts/by-psuId（兼容 /api/v1/...）。
     * 入参：psuId。
     * 返回：PromptFragmentResponse 列表。
     */
    @GetMapping("/by-psuId")
    public ResponseEntity<List<PromptFragmentResponse>> getPromptFragments(@RequestParam Long psuId) {
        List<PromptFragment> fragments = promptService.getPromptFragments(psuId);
        List<PromptFragmentResponse> responses = fragments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 创建 Prompt 片段（同步）。
     * 请求方法与路径：POST /api/prompts（兼容 /api/v1/...）。
     * 入参：CreatePromptRequest（psuId、fragmentKey、type、content、sortOrder 等）。
     * 返回：创建后的 PromptFragmentResponse。
     */
    @PostMapping
    public ResponseEntity<PromptFragmentResponse> createPromptFragment(@Valid @RequestBody CreatePromptRequest request) {
        PromptFragment fragment = promptService.createPromptFragment(request, DEFAULT_OPERATOR_ID);
        PromptFragmentResponse response = convertToResponse(fragment);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建 Prompt 片段（异步）。
     * 请求方法与路径：POST /api/prompts/async（兼容 /api/v1/...）。
     * 入参：CreatePromptRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/async")
    public ResponseEntity<String> createPromptFragmentAsync(@Valid @RequestBody CreatePromptRequest request) {
        asyncDispatchService.dispatch(() -> promptService.createPromptFragment(request, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 更新 Prompt 片段（同步）。
     * 请求方法与路径：PUT /api/prompts/by-fragmentId（兼容 /api/v1/...）。
     * 入参：fragmentId + UpdatePromptRequest（baseVersionNo、content）。
     * 返回：更新后的 PromptFragmentResponse。
     */
    @PostMapping("/by-fragmentId")
    public ResponseEntity<PromptFragmentResponse> updatePromptFragment(
            @RequestParam Long fragmentId,
            @Valid @RequestBody UpdatePromptRequest requestBody) {
        PromptFragment fragment = promptService.updatePromptFragment(
            fragmentId,
            requestBody.getBaseVersionNo(),
            requestBody.getContent(),
            DEFAULT_OPERATOR_ID
        );
        PromptFragmentResponse response = convertToResponse(fragment);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新 Prompt 片段（异步）。
     * 请求方法与路径：PUT /api/prompts/by-fragmentId/async（兼容 /api/v1/...）。
     * 入参：fragmentId + UpdatePromptRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-fragmentId/async")
    public ResponseEntity<String> updatePromptFragmentAsync(
            @RequestParam Long fragmentId,
            @Valid @RequestBody UpdatePromptRequest requestBody) {
        asyncDispatchService.dispatch(() -> promptService.updatePromptFragment(
            fragmentId,
            requestBody.getBaseVersionNo(),
            requestBody.getContent(),
            DEFAULT_OPERATOR_ID
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 删除 Prompt 片段（同步）。
     * 请求方法与路径：DELETE /api/prompts/by-fragmentId（兼容 /api/v1/...）。
     * 入参：fragmentId。
     * 返回：200 OK（无 body）。
     */
    @DeleteMapping("/by-fragmentId")
    public ResponseEntity<Void> deletePromptFragment(@RequestParam Long fragmentId) {
        promptService.deletePromptFragment(fragmentId, DEFAULT_OPERATOR_ID);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除 Prompt 片段（异步）。
     * 请求方法与路径：DELETE /api/prompts/by-fragmentId/async（兼容 /api/v1/...）。
     * 入参：fragmentId。
     * 返回：202 ACCEPTED。
     */
    @DeleteMapping("/by-fragmentId/async")
    public ResponseEntity<String> deletePromptFragmentAsync(@RequestParam Long fragmentId) {
        asyncDispatchService.dispatch(() -> promptService.deletePromptFragment(fragmentId, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 定版 Prompt 片段（同步）。
     * 请求方法与路径：POST /api/prompts/by-fragmentId/finalize（兼容 /api/v1/...）。
     * 入参：fragmentId。
     * 返回：定版后的 PromptFragmentResponse。
     */
    @PostMapping("/by-fragmentId/finalize")
    public ResponseEntity<PromptFragmentResponse> finalizePromptFragment(@RequestParam Long fragmentId) {
        PromptFragment fragment = promptService.finalizePromptFragment(fragmentId, DEFAULT_OPERATOR_ID);
        PromptFragmentResponse response = convertToResponse(fragment);
        return ResponseEntity.ok(response);
    }

    /**
     * 定版 Prompt 片段（异步）。
     * 请求方法与路径：POST /api/prompts/by-fragmentId/finalize/async（兼容 /api/v1/...）。
     * 入参：fragmentId。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-fragmentId/finalize/async")
    public ResponseEntity<String> finalizePromptFragmentAsync(@RequestParam Long fragmentId) {
        asyncDispatchService.dispatch(() -> promptService.finalizePromptFragment(fragmentId, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 同步测试 Prompt 渲染效果。
     * 请求方法与路径：POST /api/prompts/by-psuId/test（兼容 /api/v1/...）。
     * 入参：psuId + requestBody（渲染上下文变量）。
     * 返回：PromptTestResponse（渲染文本、缺失变量、诊断信息）。
     */
    @PostMapping("/by-psuId/test")
    public ResponseEntity<PromptTestResponse> testPrompt(@RequestParam Long psuId, @RequestBody Map<String, Object> requestBody) {
        // 统一返回结构化测试结果，便于前端展示渲染文本与缺失变量
        PromptTestResponse result = promptService.testPrompt(psuId, requestBody);
        return ResponseEntity.ok(result);
    }

    /**
     * 异步触发 Prompt 测试。
     * 请求方法与路径：POST /api/prompts/by-psuId/test/async（兼容 /api/v1/...）。
     * 入参：psuId + requestBody。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-psuId/test/async")
    public ResponseEntity<String> testPromptAsync(@RequestParam Long psuId, @RequestBody Map<String, Object> requestBody) {
        asyncDispatchService.dispatch(() -> promptService.testPrompt(psuId, requestBody));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 将Prompt片段实体转换为响应DTO
     */
    private PromptFragmentResponse convertToResponse(PromptFragment fragment) {
        Objects.requireNonNull(fragment, "fragment不能为空");
        PromptFragmentResponse response = new PromptFragmentResponse();
        BeanUtils.copyProperties(fragment, response);
        response.setType(fragment.getType().getCode());
        return response;
    }
}




