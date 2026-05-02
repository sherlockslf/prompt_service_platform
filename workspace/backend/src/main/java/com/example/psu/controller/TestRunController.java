package com.example.psu.controller;

import com.example.psu.dto.request.TestRunRequest;
import com.example.psu.dto.response.TestRunResponse;
import com.example.psu.dto.response.TestRunSummaryResponse;
import com.example.psu.service.TestRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 测试运行控制器
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供测试运行、详情与历史查询接口
 */
@RestController
@RequestMapping({"/api/test-runs", "/api/v1/test-runs"})
public class TestRunController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    @Autowired
    private TestRunService testRunService;

    /**
     * 开发侧/业务侧Prompt测试页面执行测试数据集运行
     * 参数：psuId-PSU数据库ID，datasetId-数据集ID，requestBody-额外运行配置
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
     * 测试运行结果页面查看特定测试运行详情
     * 参数：runId-测试运行ID
     */
    @GetMapping("/{runId}")
    public ResponseEntity<TestRunResponse> getRun(@PathVariable Long runId) {
        return ResponseEntity.ok(testRunService.getRun(runId));
    }

    /**
     * 查询测试运行历史（默认返回最近50条）
     * 参数：psuId-PSU数据库ID，datasetId-可选数据集ID
     */
    @GetMapping
    public ResponseEntity<List<TestRunSummaryResponse>> listRuns(
        @RequestParam Long psuId,
        @RequestParam(required = false) Long datasetId
    ) {
        return ResponseEntity.ok(testRunService.listRuns(psuId, datasetId));
    }
}


