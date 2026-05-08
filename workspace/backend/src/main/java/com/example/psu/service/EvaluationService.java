package com.example.psu.service;

import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.request.CreateEvaluationTaskRequest;
import com.example.psu.dto.response.EvaluationItemResultResponse;
import com.example.psu.dto.response.EvaluationReportResponse;
import com.example.psu.dto.response.EvaluationTaskResponse;
import com.example.psu.entity.EvaluationItemResult;
import com.example.psu.entity.EvaluationReport;
import com.example.psu.entity.EvaluationTask;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.TestDataset;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.EvaluationItemResultRepository;
import com.example.psu.repository.EvaluationReportRepository;
import com.example.psu.repository.EvaluationTaskRepository;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.TestDatasetRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 评估服务
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供评估任务创建、执行与报告查询能力
 */
@Service
@SuppressWarnings("null")
public class EvaluationService {
    private static final String TASK_CREATED = "CREATED";
    private static final String TASK_RUNNING = "RUNNING";
    private static final String TASK_SUCCESS = "SUCCESS";
    private static final String TASK_FAILED = "FAILED";
    private static final String TASK_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";

    private final EvaluationTaskRepository evaluationTaskRepository;
    private final EvaluationItemResultRepository evaluationItemResultRepository;
    private final EvaluationReportRepository evaluationReportRepository;
    private final PsuRepository psuRepository;
    private final TestDatasetRepository testDatasetRepository;
    private final PromptCompositionRepository promptCompositionRepository;
    private final CompositionService compositionService;
    private final ObjectMapper objectMapper;

    public EvaluationService(
        EvaluationTaskRepository evaluationTaskRepository,
        EvaluationItemResultRepository evaluationItemResultRepository,
        EvaluationReportRepository evaluationReportRepository,
        PsuRepository psuRepository,
        TestDatasetRepository testDatasetRepository,
        PromptCompositionRepository promptCompositionRepository,
        CompositionService compositionService,
        ObjectMapper objectMapper
    ) {
        this.evaluationTaskRepository = evaluationTaskRepository;
        this.evaluationItemResultRepository = evaluationItemResultRepository;
        this.evaluationReportRepository = evaluationReportRepository;
        this.psuRepository = psuRepository;
        this.testDatasetRepository = testDatasetRepository;
        this.promptCompositionRepository = promptCompositionRepository;
        this.compositionService = compositionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建评估任务
     */
    @Transactional
    public EvaluationTaskResponse createTask(CreateEvaluationTaskRequest request, Long operatorId) {
        RequestValidationUtils.requireNonNull(request, "request");
        RequestValidationUtils.requireNonNull(request.getPsuId(), "psuId");
        RequestValidationUtils.requireNonNull(request.getDatasetId(), "datasetId");
        Long safePsuId = Objects.requireNonNull(request.getPsuId());
        Long safeDatasetId = Objects.requireNonNull(request.getDatasetId());
        psuRepository.findById(safePsuId).orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        TestDataset dataset = testDatasetRepository.findById(safeDatasetId)
            .orElseThrow(() -> new RuntimeException("Test dataset not found: " + safeDatasetId));
        if (!Objects.equals(dataset.getPsuId(), safePsuId)) {
            throw new RuntimeException("测试集不属于当前PSU");
        }
        List<Map<String, Object>> cases = parseDatasetCases(dataset.getDataContent());

        EvaluationTask task = new EvaluationTask();
        task.setPsuId(safePsuId);
        task.setDatasetId(safeDatasetId);
        task.setStatus(TASK_CREATED);
        task.setTotalCases(cases.size());
        task.setCreatedBy(operatorId == null ? 0L : operatorId);
        return toTaskResponse(evaluationTaskRepository.save(task), false);
    }

    /**
     * 执行评估任务
     */
    @Transactional
    public EvaluationTaskResponse runTask(Long taskId, Long operatorId) {
        RequestValidationUtils.requireNonNull(taskId, "taskId");
        EvaluationTask task = evaluationTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Evaluation task not found: " + taskId));
        if (TASK_RUNNING.equals(task.getStatus())) {
            throw new RuntimeException("任务正在执行中，请稍后重试");
        }
        if (TASK_SUCCESS.equals(task.getStatus()) || TASK_FAILED.equals(task.getStatus()) || TASK_PARTIAL_SUCCESS.equals(task.getStatus())) {
            throw new RuntimeException("任务已执行完成，不允许重复执行");
        }

        TestDataset dataset = testDatasetRepository.findById(task.getDatasetId())
            .orElseThrow(() -> new RuntimeException("Test dataset not found: " + task.getDatasetId()));
        PromptComposition composition = promptCompositionRepository.findByPsuId(task.getPsuId())
            .orElseThrow(() -> new RuntimeException("Composition not found for PSU: " + task.getPsuId()));
        List<Map<String, Object>> cases = parseDatasetCases(dataset.getDataContent());

        // 开始执行前清理旧明细，确保重试时结果集合唯一。
        evaluationItemResultRepository.deleteByTaskId(task.getId());
        task.setStatus(TASK_RUNNING);
        task.setErrorMessage(null);
        task.setStartedAt(LocalDateTime.now());
        task.setFinishedAt(null);
        task.setProcessedCases(0);
        task.setSuccessCases(0);
        task.setFailedCases(0);
        evaluationTaskRepository.save(task);

        int successCases = 0;
        int failedCases = 0;
        BigDecimal scoreSum = BigDecimal.ZERO;
        int index = 1;
        String firstError = null;
        for (Map<String, Object> testCase : cases) {
            // 单条case异常隔离，避免整批任务中断。
            EvaluationItemResult item = new EvaluationItemResult();
            item.setTaskId(task.getId());
            item.setCaseId(String.valueOf(testCase.getOrDefault("caseId", "case-" + index)));
            item.setCaseName(String.valueOf(testCase.getOrDefault("name", item.getCaseId())));
            Map<String, Object> input = extractCaseInput(testCase);
            item.setInputJson(writeJson(input));
            try {
                CompositionRenderRequest renderRequest = new CompositionRenderRequest();
                renderRequest.setCompositionId(composition.getId());
                renderRequest.setInput(input);
                var render = compositionService.render(task.getPsuId(), renderRequest);

                boolean hasMissing = render.getMissingVars() != null && !render.getMissingVars().isEmpty();
                item.setRenderedPrompt(render.getRenderedPrompt());
                item.setActualOutput(hasMissing ? null : "MOCK_OUTPUT: " + render.getRenderedPrompt());
                item.setStatus(hasMissing ? TASK_FAILED : TASK_SUCCESS);
                item.setReason(hasMissing ? "缺失变量: " + String.join(",", render.getMissingVars()) : "规则评分通过");

                BigDecimal relevance = hasMissing ? bd(60) : bd(100);
                BigDecimal completeness = hasMissing
                    ? bd(Math.max(0, 100 - render.getMissingVars().size() * 20))
                    : bd(100);
                BigDecimal format = hasMissing ? bd(70) : bd(100);
                BigDecimal total = relevance.add(completeness).add(format).divide(bd(3), 2, RoundingMode.HALF_UP);

                item.setRelevanceScore(relevance);
                item.setCompletenessScore(completeness);
                item.setFormatScore(format);
                item.setTotalScore(total);
                scoreSum = scoreSum.add(total);

                if (hasMissing) {
                    failedCases++;
                    if (firstError == null) {
                        firstError = item.getReason();
                    }
                } else {
                    successCases++;
                }
            } catch (Exception ex) {
                item.setStatus(TASK_FAILED);
                item.setReason(safeErrorMessage(ex));
                item.setRelevanceScore(bd(0));
                item.setCompletenessScore(bd(0));
                item.setFormatScore(bd(0));
                item.setTotalScore(bd(0));
                failedCases++;
                if (firstError == null) {
                    firstError = item.getReason();
                }
            }
            evaluationItemResultRepository.save(item);
            index++;
        }

        int total = cases.size();
        int processed = successCases + failedCases;
        BigDecimal avg = total == 0 ? bd(0) : scoreSum.divide(bd(total), 2, RoundingMode.HALF_UP);
        task.setProcessedCases(processed);
        task.setSuccessCases(successCases);
        task.setFailedCases(failedCases);
        task.setAverageScore(avg);
        task.setErrorMessage(firstError);
        task.setFinishedAt(LocalDateTime.now());
        task.setStatus(resolveTaskStatus(total, failedCases));
        evaluationTaskRepository.save(task);

        // 任务完成后生成（或覆盖）报告，报告结构先按MVP字段输出。
        EvaluationReport report = evaluationReportRepository.findByTaskId(task.getId()).orElseGet(EvaluationReport::new);
        report.setTaskId(task.getId());
        report.setOverallScore(avg);
        report.setPassRate(total == 0 ? bd(100) : bd(successCases * 100.0 / total));
        report.setSummaryJson(buildSummaryJson(task));
        evaluationReportRepository.save(report);
        return toTaskResponse(task, true);
    }

    /**
     * 查询任务详情
     */
    public EvaluationTaskResponse getTask(Long taskId) {
        RequestValidationUtils.requireNonNull(taskId, "taskId");
        EvaluationTask task = evaluationTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Evaluation task not found: " + taskId));
        return toTaskResponse(task, true);
    }

    /**
     * 查询任务历史（按PSU，支持按测试集筛选）
     */
    public List<EvaluationTaskResponse> listTasks(Long psuId, Long datasetId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        // 先确认PSU存在，避免无效参数直接走任务表造成误导。
        psuRepository.findById(psuId).orElseThrow(() -> new RuntimeException("PSU not found: " + psuId));
        // datasetId为空时返回PSU下最近任务；不为空时收敛到指定测试集。
        List<EvaluationTask> tasks = datasetId == null
            ? evaluationTaskRepository.findTop50ByPsuIdOrderByCreatedAtDesc(psuId)
            : evaluationTaskRepository.findTop50ByPsuIdAndDatasetIdOrderByCreatedAtDesc(psuId, datasetId);
        List<EvaluationTaskResponse> result = new ArrayList<>();
        for (EvaluationTask task : tasks) {
            // 历史列表只需要汇总字段，不回传明细列表，减少前端首屏载荷。
            result.add(toTaskResponse(task, false));
        }
        return result;
    }

    /**
     * 查询评估报告
     */
    public EvaluationReportResponse getReport(Long reportId) {
        RequestValidationUtils.requireNonNull(reportId, "reportId");
        EvaluationReport report = evaluationReportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Evaluation report not found: " + reportId));
        EvaluationReportResponse response = new EvaluationReportResponse();
        response.setId(report.getId());
        response.setTaskId(report.getTaskId());
        response.setOverallScore(report.getOverallScore());
        response.setPassRate(report.getPassRate());
        response.setSummaryJson(report.getSummaryJson());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());

        List<EvaluationItemResult> allItems = evaluationItemResultRepository.findByTaskIdOrderByIdAsc(report.getTaskId());
        for (EvaluationItemResult item : allItems) {
            if (!TASK_FAILED.equals(item.getStatus())) {
                continue;
            }
            response.getIssueItems().add(toItemResponse(item));
            if (response.getIssueItems().size() >= 5) {
                break;
            }
        }
        return response;
    }

    private EvaluationTaskResponse toTaskResponse(EvaluationTask task, boolean includeItems) {
        EvaluationTaskResponse response = new EvaluationTaskResponse();
        response.setId(task.getId());
        response.setPsuId(task.getPsuId());
        response.setDatasetId(task.getDatasetId());
        response.setStatus(task.getStatus());
        response.setTotalCases(task.getTotalCases());
        response.setProcessedCases(task.getProcessedCases());
        response.setSuccessCases(task.getSuccessCases());
        response.setFailedCases(task.getFailedCases());
        response.setAverageScore(task.getAverageScore());
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedAt(task.getCreatedAt());
        response.setStartedAt(task.getStartedAt());
        response.setFinishedAt(task.getFinishedAt());
        evaluationReportRepository.findByTaskId(task.getId()).ifPresent(report -> response.setReportId(report.getId()));
        if (includeItems) {
            List<EvaluationItemResult> items = evaluationItemResultRepository.findByTaskIdOrderByIdAsc(task.getId());
            for (EvaluationItemResult item : items) {
                response.getItems().add(toItemResponse(item));
            }
        }
        return response;
    }

    private EvaluationItemResultResponse toItemResponse(EvaluationItemResult item) {
        EvaluationItemResultResponse response = new EvaluationItemResultResponse();
        response.setId(item.getId());
        response.setCaseId(item.getCaseId());
        response.setCaseName(item.getCaseName());
        response.setInput(readMap(item.getInputJson()));
        response.setRenderedPrompt(item.getRenderedPrompt());
        response.setActualOutput(item.getActualOutput());
        response.setStatus(item.getStatus());
        response.setRelevanceScore(item.getRelevanceScore());
        response.setCompletenessScore(item.getCompletenessScore());
        response.setFormatScore(item.getFormatScore());
        response.setTotalScore(item.getTotalScore());
        response.setReason(item.getReason());
        return response;
    }

    private List<Map<String, Object>> parseDatasetCases(String dataContent) {
        if (dataContent == null || dataContent.isBlank()) {
            return new ArrayList<>();
        }
        try {
            if (dataContent.trim().startsWith("[")) {
                return objectMapper.readValue(dataContent, new TypeReference<List<Map<String, Object>>>() {});
            }
            Map<String, Object> object = objectMapper.readValue(dataContent, new TypeReference<Map<String, Object>>() {});
            if (object.containsKey("cases") && object.get("cases") instanceof List<?> cases) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : cases) {
                    if (item instanceof Map<?, ?> mapItem) {
                        Map<String, Object> mapped = new HashMap<>();
                        mapItem.forEach((k, v) -> mapped.put(String.valueOf(k), v));
                        result.add(mapped);
                    }
                }
                return result;
            }
            List<Map<String, Object>> single = new ArrayList<>();
            single.add(object);
            return single;
        } catch (Exception e) {
            throw new RuntimeException("测试集JSON格式错误");
        }
    }

    private Map<String, Object> extractCaseInput(Map<String, Object> testCase) {
        Object input = testCase.get("input");
        if (input instanceof Map<?, ?> inputMap) {
            Map<String, Object> result = new HashMap<>();
            inputMap.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        Map<String, Object> copy = new HashMap<>(testCase);
        copy.remove("caseId");
        copy.remove("name");
        copy.remove("expected");
        return copy;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> readMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String buildSummaryJson(EvaluationTask task) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("taskId", task.getId());
        summary.put("status", task.getStatus());
        summary.put("totalCases", task.getTotalCases());
        summary.put("successCases", task.getSuccessCases());
        summary.put("failedCases", task.getFailedCases());
        summary.put("averageScore", task.getAverageScore());
        summary.put("errorMessage", task.getErrorMessage());
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String resolveTaskStatus(int totalCases, int failedCases) {
        if (totalCases <= 0) {
            return TASK_SUCCESS;
        }
        if (failedCases <= 0) {
            return TASK_SUCCESS;
        }
        if (failedCases >= totalCases) {
            return TASK_FAILED;
        }
        return TASK_PARTIAL_SUCCESS;
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String safeErrorMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "评估执行异常";
        }
        return ex.getMessage();
    }
}
