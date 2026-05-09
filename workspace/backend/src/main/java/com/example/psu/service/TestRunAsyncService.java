package com.example.psu.service;

import com.example.psu.dto.request.TestRunRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 测试运行异步执行服务。
 */
@Service
public class TestRunAsyncService {

    private static final Logger log = LoggerFactory.getLogger(TestRunAsyncService.class);

    private final TestRunService testRunService;

    public TestRunAsyncService(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    @Async("taskExecutor")
    public void runDatasetAsync(Long psuId, Long datasetId, TestRunRequest request, Long userId) {
        try {
            testRunService.runDataset(psuId, datasetId, request, userId);
        } catch (Exception ex) {
            log.error("Async test run failed, psuId={}, datasetId={}", psuId, datasetId, ex);
        }
    }
}
