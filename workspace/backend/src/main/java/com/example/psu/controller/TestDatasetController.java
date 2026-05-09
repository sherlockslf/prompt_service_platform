package com.example.psu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.psu.dto.request.TestDatasetCreateRequest;
import com.example.psu.entity.TestDataset;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.TestDatasetService;

import jakarta.validation.Valid;

/**
 * 测试数据集管理控制器
 */
@RestController
@RequestMapping("/api/test-datasets")
public class TestDatasetController {
    
    @Autowired
    private TestDatasetService testDatasetService;
    @Autowired
    private AsyncDispatchService asyncDispatchService;
    
    /**
     * 查询指定 PSU 的测试数据集列表。
     * 请求方法与路径：GET /api/test-datasets（兼容 /api/v1/...）。
     * 入参：psuId。
     * 返回：TestDataset 列表。
     */
    @GetMapping
    public ResponseEntity<List<TestDataset>> getTestDatasets(@RequestParam Long psuId) {
        List<TestDataset> datasets = testDatasetService.getTestDatasets(psuId);
        return ResponseEntity.ok(datasets);
    }
    
    /**
     * 创建测试数据集（同步）。
     * 请求方法与路径：POST /api/test-datasets（兼容 /api/v1/...）。
     * 入参：psuId + TestDatasetCreateRequest（name、description、dataContent）。
     * 返回：创建后的 TestDataset。
     */
    @PostMapping
    public ResponseEntity<TestDataset> createTestDataset(
            @RequestParam Long psuId,
            @Valid @RequestBody TestDatasetCreateRequest request) {
        TestDataset dataset = testDatasetService.createTestDataset(psuId, request);
        return ResponseEntity.ok(dataset);
    }

    /**
     * 创建测试数据集（异步）。
     * 请求方法与路径：POST /api/test-datasets/async（兼容 /api/v1/...）。
     * 入参：psuId + TestDatasetCreateRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/async")
    public ResponseEntity<String> createTestDatasetAsync(
            @RequestParam Long psuId,
            @Valid @RequestBody TestDatasetCreateRequest request) {
        asyncDispatchService.dispatch(() -> testDatasetService.createTestDataset(psuId, request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 更新测试数据集（同步）。
     * 请求方法与路径：PUT /api/test-datasets/by-id（兼容 /api/v1/...）。
     * 入参：id + TestDatasetCreateRequest。
     * 返回：更新后的 TestDataset。
     */
    @PostMapping("/by-id")
    public ResponseEntity<TestDataset> updateTestDataset(
            @RequestParam Long id,
            @Valid @RequestBody TestDatasetCreateRequest request) {
        TestDataset dataset = testDatasetService.updateTestDataset(id, request);
        return ResponseEntity.ok(dataset);
    }

    /**
     * 更新测试数据集（异步）。
     * 请求方法与路径：PUT /api/test-datasets/by-id/async（兼容 /api/v1/...）。
     * 入参：id + TestDatasetCreateRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-id/async")
    public ResponseEntity<String> updateTestDatasetAsync(
            @RequestParam Long id,
            @Valid @RequestBody TestDatasetCreateRequest request) {
        asyncDispatchService.dispatch(() -> testDatasetService.updateTestDataset(id, request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 删除测试数据集（同步）。
     * 请求方法与路径：DELETE /api/test-datasets/by-id（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：200 OK。
     */
    @DeleteMapping("/by-id")
    public ResponseEntity<Void> deleteTestDataset(@RequestParam Long id) {
        testDatasetService.deleteTestDataset(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除测试数据集（异步）。
     * 请求方法与路径：DELETE /api/test-datasets/by-id/async（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：202 ACCEPTED。
     */
    @DeleteMapping("/by-id/async")
    public ResponseEntity<String> deleteTestDatasetAsync(@RequestParam Long id) {
        asyncDispatchService.dispatch(() -> testDatasetService.deleteTestDataset(id));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
}




