package com.example.psu.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 评估任务异步执行服务。
 */
@Service
public class EvaluationAsyncService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationAsyncService.class);

    private final EvaluationService evaluationService;

    public EvaluationAsyncService(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Async("taskExecutor")
    public void runTaskAsync(Long taskId, Long operatorId) {
        try {
            evaluationService.runTask(taskId, operatorId);
        } catch (Exception ex) {
            log.error("Async evaluation task failed, taskId={}", taskId, ex);
        }
    }
}
