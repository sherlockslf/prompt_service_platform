package com.example.psu.controller;

import com.example.psu.dto.request.CreateEvaluationTaskRequest;
import com.example.psu.dto.response.EvaluationReportResponse;
import com.example.psu.dto.response.EvaluationTaskResponse;
import com.example.psu.service.EvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 评估中心控制器
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供评估任务创建、执行、查询与报告查看接口
 */
@RestController
@RequestMapping({"/api/evaluations", "/api/v1/evaluations"})
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * 创建评估任务
     */
    @PostMapping("/tasks")
    public ResponseEntity<EvaluationTaskResponse> createTask(@RequestBody CreateEvaluationTaskRequest request) {
        // 当前阶段先使用系统默认操作者ID，后续可接入登录态用户信息。
        return ResponseEntity.ok(evaluationService.createTask(request, 0L));
    }

    /**
     * 执行评估任务
     */
    @PostMapping("/tasks/{id}/run")
    public ResponseEntity<EvaluationTaskResponse> runTask(@PathVariable Long id) {
        // 当前阶段先使用系统默认操作者ID，后续可接入登录态用户信息。
        return ResponseEntity.ok(evaluationService.runTask(id, 0L));
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<EvaluationTaskResponse> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getTask(id));
    }

    /**
     * 查询任务历史列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<EvaluationTaskResponse>> listTasks(
        @RequestParam Long psuId,
        @RequestParam(required = false) Long datasetId
    ) {
        return ResponseEntity.ok(evaluationService.listTasks(psuId, datasetId));
    }

    /**
     * 查询评估报告
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<EvaluationReportResponse> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getReport(id));
    }
}
