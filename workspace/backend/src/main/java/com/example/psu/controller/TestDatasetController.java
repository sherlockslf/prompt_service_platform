package com.example.psu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.example.psu.dto.request.TestDatasetCreateRequest;
import com.example.psu.entity.TestDataset;
import com.example.psu.service.TestDatasetService;

import jakarta.validation.Valid;

/**
 * 测试数据集管理控制器
 */
@RestController
@RequestMapping({"/api/test-datasets", "/api/v1/test-datasets"})
public class TestDatasetController {
    
    @Autowired
    private TestDatasetService testDatasetService;
    
    /**
     * 开发侧Schema编辑器页面获取指定PSU的测试数据集列表
     * 参数：psuId-PSU数据库ID
     */
    @GetMapping
    public ResponseEntity<List<TestDataset>> getTestDatasets(@RequestParam Long psuId) {
        List<TestDataset> datasets = testDatasetService.getTestDatasets(psuId);
        return ResponseEntity.ok(datasets);
    }
    
    /**
     * 开发侧Schema编辑器页面创建新测试数据集
     * 参数：psuId-PSU数据库ID，name-数据集名称，description-描述，dataContent-JSON测试数据
     */
    @PostMapping
    public ResponseEntity<TestDataset> createTestDataset(
            @RequestParam Long psuId,
            @Valid @RequestBody TestDatasetCreateRequest request) {
        TestDataset dataset = testDatasetService.createTestDataset(psuId, request);
        return ResponseEntity.ok(dataset);
    }
    
    /**
     * 开发侧Schema编辑器页面编辑更新测试数据集
     * 参数：id-数据集ID，name/description/dataContent-更新字段
     */
    @PutMapping("/{id}")
    public ResponseEntity<TestDataset> updateTestDataset(
            @PathVariable Long id,
            @Valid @RequestBody TestDatasetCreateRequest request) {
        TestDataset dataset = testDatasetService.updateTestDataset(id, request);
        return ResponseEntity.ok(dataset);
    }
    
    /**
     * 开发侧Schema编辑器页面删除测试数据集
     * 参数：id-数据集ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestDataset(@PathVariable Long id) {
        testDatasetService.deleteTestDataset(id);
        return ResponseEntity.ok().build();
    }
}


