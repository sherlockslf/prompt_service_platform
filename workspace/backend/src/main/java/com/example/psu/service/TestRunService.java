package com.example.psu.service;

import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.request.TestRunRequest;
import com.example.psu.dto.response.CompositionRenderResponse;
import com.example.psu.dto.response.TestRunResponse;
import com.example.psu.dto.response.TestRunSummaryResponse;
import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.TestDataset;
import com.example.psu.entity.TestRun;
import com.example.psu.entity.TestRunItem;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.TestDatasetRepository;
import com.example.psu.repository.TestRunItemRepository;
import com.example.psu.repository.TestRunRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * 测试运行服务
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供测试集运行、详情查询与运行历史查询能力
 */
@Service
public class TestRunService {
    private static final String RUNNING_STATUS = "RUNNING";
    private static final String SUCCESS_STATUS = "SUCCESS";
    private static final String FAILED_STATUS = "FAILED";
    private static final String PARTIAL_SUCCESS_STATUS = "PARTIAL_SUCCESS";

    @Autowired
    private PsuRepository psuRepository;

    @Autowired
    private TestDatasetRepository testDatasetRepository;

    @Autowired
    private TestRunRepository testRunRepository;

    @Autowired
    private TestRunItemRepository testRunItemRepository;

    @Autowired
    private PromptCompositionRepository compositionRepository;

    @Autowired
    private CompositionService compositionService;

    @Autowired
    private JsonSchemaRepository jsonSchemaRepository;

    @Autowired
    private LlmChatService llmChatService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 运行测试集
     */
    @Transactional
    public TestRunResponse runDataset(Long psuId, Long datasetId, TestRunRequest request, Long userId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonNull(datasetId, "datasetId");
        Long safePsuId = Objects.requireNonNull(psuId);
        Long safeDatasetId = Objects.requireNonNull(datasetId);
        // 校验PSU和测试集，确保运行上下文正确。
        psuRepository.findById(safePsuId).orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        TestDataset dataset = testDatasetRepository.findById(safeDatasetId)
            .orElseThrow(() -> new RuntimeException("Test dataset not found: " + safeDatasetId));
        if (!dataset.getPsuId().equals(safePsuId)) {
            throw new RuntimeException("测试集不属于当前PSU");
        }

        PromptComposition composition = resolveComposition(safePsuId, request == null ? null : request.getCompositionId());
        List<Map<String, Object>> cases = parseDatasetCases(dataset.getDataContent());
        JsonNode schemaRoot = loadSchemaRoot(safePsuId);

        TestRun run = new TestRun();
        run.setPsuId(safePsuId);
        run.setDatasetId(safeDatasetId);
        run.setCompositionId(composition.getId());
        run.setStatus(RUNNING_STATUS);
        run.setExceptionReason(null);
        run.setCreatedBy(userId);
        run = testRunRepository.save(run);

        int success = 0;
        int failed = 0;
        List<TestRunResponse.Item> items = new ArrayList<>();
        int index = 1;
        String firstExceptionReason = null;
        for (Map<String, Object> testCase : cases) {
            // 逐条用例渲染并记录详细运行结果。
            String caseId = String.valueOf(testCase.getOrDefault("caseId", "case-" + index));
            String name = String.valueOf(testCase.getOrDefault("name", caseId));
            Map<String, Object> input = extractCaseInput(testCase);

            String renderedPrompt = null;
            String actualOutput = null;
            String error = null;
            String exceptionReason = null;
            String itemStatus;
            boolean itemSuccess;
            int latency;
            long begin = System.currentTimeMillis();
            try {
                // 对单条用例做异常隔离，确保单条失败不阻断整批测试。
                CompositionRenderRequest renderRequest = new CompositionRenderRequest();
                renderRequest.setCompositionId(composition.getId());
                renderRequest.setInput(input);
                validateInputAgainstSchema(input, schemaRoot);
                CompositionRenderResponse renderResponse = compositionService.render(safePsuId, renderRequest);
                latency = (int) (System.currentTimeMillis() - begin);
                renderedPrompt = renderResponse.getRenderedPrompt();
                itemSuccess = renderResponse.getMissingVars() == null || renderResponse.getMissingVars().isEmpty();
                error = itemSuccess ? null : "缺失变量: " + String.join(",", renderResponse.getMissingVars());
                actualOutput = itemSuccess ? llmChatService.chatOnce(renderedPrompt) : null;
                itemStatus = itemSuccess ? SUCCESS_STATUS : FAILED_STATUS;
                exceptionReason = error;
            } catch (Exception ex) {
                latency = (int) (System.currentTimeMillis() - begin);
                itemSuccess = false;
                itemStatus = FAILED_STATUS;
                error = "执行异常";
                exceptionReason = safeErrorMessage(ex);
            }

            TestRunItem runItem = new TestRunItem();
            runItem.setRunId(run.getId());
            runItem.setCaseId(caseId);
            // 记录用例名称，避免数据库非空字段缺失导致插入失败。
            runItem.setCaseName(name);
            runItem.setInputJson(writeJson(input));
            runItem.setRenderedPrompt(renderedPrompt);
            runItem.setActualOutput(actualOutput);
            runItem.setSuccess(itemSuccess);
            runItem.setStatus(itemStatus);
            runItem.setErrorMessage(error);
            runItem.setExceptionReason(exceptionReason);
            runItem.setLatencyMs(latency);
            testRunItemRepository.save(runItem);

            if (itemSuccess) {
                success++;
            } else {
                failed++;
                if (firstExceptionReason == null || firstExceptionReason.isBlank()) {
                    // 主记录保留首个异常原因，便于运行历史快速定位问题根因。
                    firstExceptionReason = exceptionReason;
                }
            }

            TestRunResponse.Item responseItem = new TestRunResponse.Item();
            responseItem.setCaseId(caseId);
            responseItem.setName(name);
            responseItem.setInput(input);
            responseItem.setRenderedPrompt(renderedPrompt);
            responseItem.setActualOutput(actualOutput);
            responseItem.setSuccess(itemSuccess);
            responseItem.setStatus(itemStatus);
            responseItem.setError(error);
            responseItem.setExceptionReason(exceptionReason);
            responseItem.setLatencyMs(latency);
            items.add(responseItem);
            index++;
        }

        run.setTotalCases(cases.size());
        run.setSuccessCases(success);
        run.setFailedCases(failed);
        run.setStatus(calculateRunStatus(cases.size(), failed));
        run.setExceptionReason(firstExceptionReason);
        testRunRepository.save(run);

        TestRunResponse response = new TestRunResponse();
        response.setRunId(run.getId());
        response.setTotalCases(cases.size());
        response.setSuccessCases(success);
        response.setFailedCases(failed);
        response.setStatus(run.getStatus());
        response.setExceptionReason(run.getExceptionReason());
        response.setItems(items);
        return response;
    }

    /**
     * 获取测试运行详情
     */
    public TestRunResponse getRun(Long runId) {
        RequestValidationUtils.requireNonNull(runId, "runId");
        Long safeRunId = Objects.requireNonNull(runId);
        // 读取主记录与明细并拼装统一响应结构。
        TestRun run = testRunRepository.findById(safeRunId)
            .orElseThrow(() -> new RuntimeException("Test run not found: " + safeRunId));
        List<TestRunItem> runItems = testRunItemRepository.findByRunIdOrderByIdAsc(safeRunId);

        TestRunResponse response = new TestRunResponse();
        response.setRunId(run.getId());
        response.setTotalCases(run.getTotalCases());
        response.setSuccessCases(run.getSuccessCases());
        response.setFailedCases(run.getFailedCases());
        response.setStatus(run.getStatus());
        response.setExceptionReason(run.getExceptionReason());

        List<TestRunResponse.Item> items = new ArrayList<>();
        for (TestRunItem runItem : runItems) {
            TestRunResponse.Item item = new TestRunResponse.Item();
            item.setCaseId(runItem.getCaseId());
            // 详情返回优先使用用例名称，缺失时回退到用例ID。
            item.setName(runItem.getCaseName() == null || runItem.getCaseName().isBlank()
                ? runItem.getCaseId()
                : runItem.getCaseName());
            item.setInput(readMap(runItem.getInputJson()));
            item.setRenderedPrompt(runItem.getRenderedPrompt());
            item.setActualOutput(runItem.getActualOutput());
            item.setSuccess(runItem.isSuccess());
            item.setStatus(runItem.getStatus());
            item.setError(runItem.getErrorMessage());
            item.setExceptionReason(runItem.getExceptionReason());
            item.setLatencyMs(runItem.getLatencyMs());
            items.add(item);
        }
        response.setItems(items);
        return response;
    }

    /**
     * 查询测试运行历史（按PSU可选按数据集筛选）
     */
    public List<TestRunSummaryResponse> listRuns(Long psuId, Long datasetId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 先校验PSU存在，避免前端误传时返回空列表造成误判。
        psuRepository.findById(safePsuId).orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        List<TestRun> runs;
        if (datasetId == null) {
            runs = testRunRepository.findTop50ByPsuIdOrderByCreatedAtDesc(safePsuId);
        } else {
            runs = testRunRepository.findTop50ByPsuIdAndDatasetIdOrderByCreatedAtDesc(safePsuId, datasetId);
        }
        List<TestRunSummaryResponse> responses = new ArrayList<>();
        for (TestRun run : runs) {
            TestRunSummaryResponse item = new TestRunSummaryResponse();
            item.setRunId(run.getId());
            item.setPsuId(run.getPsuId());
            item.setDatasetId(run.getDatasetId());
            item.setCompositionId(run.getCompositionId());
            item.setTotalCases(run.getTotalCases());
            item.setSuccessCases(run.getSuccessCases());
            item.setFailedCases(run.getFailedCases());
            item.setStatus(run.getStatus());
            item.setExceptionReason(run.getExceptionReason());
            item.setCreatedAt(run.getCreatedAt());
            responses.add(item);
        }
        return responses;
    }

    private PromptComposition resolveComposition(Long psuId, Long compositionId) {
        // 按需选择指定编排或当前草稿，保证运行来源清晰。
        if (compositionId != null) {
            PromptComposition composition = compositionRepository.findById(compositionId)
                .orElseThrow(() -> new RuntimeException("Composition not found: " + compositionId));
            if (!composition.getPsuId().equals(psuId)) {
                throw new RuntimeException("Composition does not belong to PSU");
            }
            return composition;
        }
        return compositionRepository.findByPsuId(psuId)
            .orElseThrow(() -> new RuntimeException("Composition not found for PSU: " + psuId));
    }

    private List<Map<String, Object>> parseDatasetCases(String dataContent) {
        // 兼容数组、对象和单条输入三种测试集格式。
        if (dataContent == null || dataContent.isBlank()) {
            return new ArrayList<>();
        }
        try {
            if (dataContent.trim().startsWith("[")) {
                return objectMapper.readValue(dataContent, new TypeReference<List<Map<String, Object>>>() {
                });
            }
            Map<String, Object> object = objectMapper.readValue(dataContent, new TypeReference<Map<String, Object>>() {
            });
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
        // 优先读取input字段，不存在则使用整条用例作为输入。
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

    private String calculateRunStatus(int totalCases, int failedCases) {
        if (totalCases <= 0) {
            return SUCCESS_STATUS;
        }
        if (failedCases <= 0) {
            return SUCCESS_STATUS;
        }
        if (failedCases >= totalCases) {
            return FAILED_STATUS;
        }
        return PARTIAL_SUCCESS_STATUS;
    }

    private String safeErrorMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "未知异常";
        }
        return ex.getMessage();
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    private Map<String, Object> readMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private JsonNode loadSchemaRoot(Long psuId) {
        JsonSchema schema = jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId)
            .orElseThrow(() -> new RuntimeException("当前PSU未配置JSON Schema"));
        try {
            return objectMapper.readTree(schema.getSchemaContent());
        } catch (Exception e) {
            throw new RuntimeException("JSON Schema格式错误");
        }
    }

    private void validateInputAgainstSchema(Map<String, Object> input, JsonNode schemaRoot) {
        if (schemaRoot == null || !schemaRoot.has("properties")) {
            return;
        }
        Set<String> required = new LinkedHashSet<>();
        if (schemaRoot.has("required") && schemaRoot.get("required").isArray()) {
            for (JsonNode n : schemaRoot.get("required")) {
                required.add(n.asText());
            }
        }
        List<String> errors = new ArrayList<>();
        for (String field : required) {
            if (!input.containsKey(field) || input.get(field) == null) {
                errors.add("缺少必填字段: " + field);
            }
        }
        JsonNode properties = schemaRoot.get("properties");
        input.forEach((k, v) -> {
            if (!properties.has(k)) {
                errors.add("字段不在Schema中: " + k);
                return;
            }
            JsonNode fieldSchema = properties.get(k);
            String expectedType = fieldSchema.has("type") ? fieldSchema.get("type").asText() : "";
            if (!expectedType.isBlank() && !isTypeMatched(v, expectedType)) {
                errors.add("字段类型不匹配: " + k + " 期望=" + expectedType);
            }
        });
        if (!errors.isEmpty()) {
            throw new RuntimeException(String.join("; ", errors));
        }
    }

    private boolean isTypeMatched(Object value, String expectedType) {
        return switch (expectedType) {
            case "string" -> value instanceof String;
            case "number" -> value instanceof Number;
            case "integer" -> value instanceof Integer || value instanceof Long || value instanceof Short;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List<?>;
            case "object" -> value instanceof Map<?, ?>;
            default -> true;
        };
    }
}


