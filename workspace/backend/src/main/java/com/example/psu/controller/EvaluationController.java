package com.example.psu.controller;

import com.example.psu.dto.request.CreateEvaluationTaskRequest;
import com.example.psu.dto.response.EvaluationReportResponse;
import com.example.psu.dto.response.EvaluationTaskResponse;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.EvaluationAsyncService;
import com.example.psu.service.EvaluationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评估中心控制器
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供评估任务创建、执行、查询与报告查看接口
 */
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final EvaluationService evaluationService;
    private final EvaluationAsyncService evaluationAsyncService;
    private final AsyncDispatchService asyncDispatchService;

    public EvaluationController(
        EvaluationService evaluationService,
        EvaluationAsyncService evaluationAsyncService,
        AsyncDispatchService asyncDispatchService
    ) {
        this.evaluationService = evaluationService;
        this.evaluationAsyncService = evaluationAsyncService;
        this.asyncDispatchService = asyncDispatchService;
    }

    /**
     * 创建评估任务主记录。
     * 请求方法与路径：POST /api/evaluations/tasks（兼容 /api/v1/...）。
     * 入参：CreateEvaluationTaskRequest（psuId、datasetId、评估维度等）。
     * 返回：EvaluationTaskResponse（初始状态 CREATED）。
     */
    @PostMapping("/tasks")
    public ResponseEntity<EvaluationTaskResponse> createTask(@RequestBody CreateEvaluationTaskRequest request) {
        // 当前阶段先使用系统默认操作者ID，后续可接入登录态用户信息。
        return ResponseEntity.ok(evaluationService.createTask(request, 0L));
    }

    /**
     * 异步创建评估任务。
     * 请求方法与路径：POST /api/evaluations/tasks/async（兼容 /api/v1/...）。
     * 入参：CreateEvaluationTaskRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/tasks/async")
    public ResponseEntity<String> createTaskAsync(@RequestBody CreateEvaluationTaskRequest request) {
        asyncDispatchService.dispatch(() -> evaluationService.createTask(request, 0L));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 同步执行评估任务。
     * 请求方法与路径：POST /api/evaluations/tasks/by-id/run（兼容 /api/v1/...）。
     * 入参：id（评估任务主键）。
     * 返回：EvaluationTaskResponse（包含执行后状态与统计信息）。
     */
    @PostMapping("/tasks/by-id/run")
    public ResponseEntity<EvaluationTaskResponse> runTask(@RequestParam Long id) {
        // 当前阶段先使用系统默认操作者ID，后续可接入登录态用户信息。
        return ResponseEntity.ok(evaluationService.runTask(id, 0L));
    }

    /**
     * 异步执行评估任务。
     * 请求方法与路径：POST /api/evaluations/tasks/by-id/run-async（兼容 /api/v1/...）。
     * 入参：id（评估任务主键）。
     * 返回：202 ACCEPTED + 当前任务快照。
     */
    @PostMapping("/tasks/by-id/run-async")
    public ResponseEntity<EvaluationTaskResponse> runTaskAsync(@RequestParam Long id) {
        EvaluationTaskResponse task = evaluationService.getTask(id);
        evaluationAsyncService.runTaskAsync(id, 0L);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(task);
    }

    /**
     * 查询评估任务详情。
     * 请求方法与路径：GET /api/evaluations/tasks/by-id（兼容 /api/v1/...）。
     * 入参：id（评估任务主键）。
     * 返回：EvaluationTaskResponse（可包含明细项）。
     */
    @GetMapping("/tasks/by-id")
    public ResponseEntity<EvaluationTaskResponse> getTask(@RequestParam Long id) {
        return ResponseEntity.ok(evaluationService.getTask(id));
    }

    /**
     * 按 PSU 查询评估任务历史。
     * 请求方法与路径：GET /api/evaluations/tasks（兼容 /api/v1/...）。
     * 入参：psuId（必填）、datasetId（可选筛选）。
     * 返回：评估任务列表（按时间倒序）。
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<EvaluationTaskResponse>> listTasks(
        @RequestParam Long psuId,
        @RequestParam(required = false) Long datasetId
    ) {
        return ResponseEntity.ok(evaluationService.listTasks(psuId, datasetId));
    }

    /**
     * 查询评估报告详情。
     * 请求方法与路径：GET /api/evaluations/reports/by-id（兼容 /api/v1/...）。
     * 入参：id（报告主键）。
     * 返回：EvaluationReportResponse（总体得分、问题样本等）。
     */
    @GetMapping("/reports/by-id")
    public ResponseEntity<EvaluationReportResponse> getReport(@RequestParam Long id) {
        return ResponseEntity.ok(evaluationService.getReport(id));
    }
}


