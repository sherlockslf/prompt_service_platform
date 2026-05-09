package com.example.psu.controller;

import com.example.psu.dto.request.TestRunRequest;
import com.example.psu.dto.response.TestRunResponse;
import com.example.psu.dto.response.TestRunSummaryResponse;
import com.example.psu.service.TestRunAsyncService;
import com.example.psu.service.TestRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 测试运行控制器
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供测试运行、详情与历史查询接口
 */
@RestController
@RequestMapping("/api/test-runs")
public class TestRunController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    @Autowired
    private TestRunService testRunService;
    @Autowired
    private TestRunAsyncService testRunAsyncService;

    /**
     * 同步执行测试数据集并返回完整运行结果。
     * 请求方法与路径：POST /api/test-runs（兼容 /api/v1/test-runs）。
     * 入参：psuId、datasetId、TestRunRequest（可选 compositionId 等）。
     * 返回：TestRunResponse（汇总状态 + 每条用例明细）。
     * 说明：适合需要立即拿到运行明细并在页面直接展示的场景。
     */
    @PostMapping
    public ResponseEntity<TestRunResponse> runDataset(
        @RequestParam Long psuId,
        @RequestParam Long datasetId,
        @RequestBody(required = false) TestRunRequest request
    ) {
        TestRunResponse response = testRunService.runDataset(psuId, datasetId, request, DEFAULT_OPERATOR_ID);
        return ResponseEntity.ok(response);
    }

    /**
     * 异步触发测试数据集运行任务。
     * 请求方法与路径：POST /api/test-runs/async（兼容 /api/v1/...）。
     * 入参：psuId、datasetId、TestRunRequest。
     * 返回：202 ACCEPTED。
     * 说明：任务进入异步队列，调用方可后续查询运行历史或详情接口获取结果。
     */
    @PostMapping("/async")
    public ResponseEntity<String> runDatasetAsync(
        @RequestParam Long psuId,
        @RequestParam Long datasetId,
        @RequestBody(required = false) TestRunRequest request
    ) {
        testRunAsyncService.runDatasetAsync(psuId, datasetId, request, DEFAULT_OPERATOR_ID);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    /**
     * 查询单次测试运行详情。
     * 请求方法与路径：GET /api/test-runs/by-runId（兼容 /api/v1/...）。
     * 入参：runId（测试运行主键）。
     * 返回：TestRunResponse（包含运行汇总与各 case 结果）。
     */
    @GetMapping("/by-runId")
    public ResponseEntity<TestRunResponse> getRun(@RequestParam Long runId) {
        return ResponseEntity.ok(testRunService.getRun(runId));
    }

    /**
     * 查询测试运行历史列表。
     * 请求方法与路径：GET /api/test-runs（兼容 /api/v1/...）。
     * 入参：psuId（必填）、datasetId（可选）。
     * 返回：按时间倒序的最近运行摘要列表。
     * 说明：服务层默认限制最近 50 条，避免历史查询拖慢首页加载。
     */
    @GetMapping
    public ResponseEntity<List<TestRunSummaryResponse>> listRuns(
        @RequestParam Long psuId,
        @RequestParam(required = false) Long datasetId
    ) {
        return ResponseEntity.ok(testRunService.listRuns(psuId, datasetId));
    }
}




