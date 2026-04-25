package com.example.psu.service;

import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.request.TestRunRequest;
import com.example.psu.dto.response.CompositionRenderResponse;
import com.example.psu.dto.response.TestRunResponse;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.TestDataset;
import com.example.psu.entity.TestRun;
import com.example.psu.entity.TestRunItem;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.TestDatasetRepository;
import com.example.psu.repository.TestRunItemRepository;
import com.example.psu.repository.TestRunRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 测试运行服务
 */
@Service
public class TestRunService {

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

        TestRun run = new TestRun();
        run.setPsuId(safePsuId);
        run.setDatasetId(safeDatasetId);
        run.setCompositionId(composition.getId());
        run.setCreatedBy(userId);
        run = testRunRepository.save(run);

        int success = 0;
        int failed = 0;
        List<TestRunResponse.Item> items = new ArrayList<>();
        int index = 1;
        for (Map<String, Object> testCase : cases) {
            // 逐条用例渲染并记录详细运行结果。
            String caseId = String.valueOf(testCase.getOrDefault("caseId", "case-" + index));
            String name = String.valueOf(testCase.getOrDefault("name", caseId));
            Map<String, Object> input = extractCaseInput(testCase);

            CompositionRenderRequest renderRequest = new CompositionRenderRequest();
            renderRequest.setCompositionId(composition.getId());
            renderRequest.setInput(input);
            long begin = System.currentTimeMillis();
            CompositionRenderResponse renderResponse = compositionService.render(safePsuId, renderRequest);
            int latency = (int) (System.currentTimeMillis() - begin);

            boolean itemSuccess = renderResponse.getMissingVars() == null || renderResponse.getMissingVars().isEmpty();
            String error = itemSuccess ? null : "缺失变量: " + String.join(",", renderResponse.getMissingVars());
            String modelOutput = itemSuccess ? "MOCK_OUTPUT: " + renderResponse.getRenderedPrompt() : null;

            TestRunItem runItem = new TestRunItem();
            runItem.setRunId(run.getId());
            runItem.setCaseId(caseId);
            // 记录用例名称，避免数据库非空字段缺失导致插入失败。
            runItem.setCaseName(name);
            runItem.setInputJson(writeJson(input));
            runItem.setRenderedPrompt(renderResponse.getRenderedPrompt());
            runItem.setModelOutput(modelOutput);
            runItem.setSuccess(itemSuccess);
            runItem.setErrorMessage(error);
            runItem.setLatencyMs(latency);
            testRunItemRepository.save(runItem);

            if (itemSuccess) {
                success++;
            } else {
                failed++;
            }

            TestRunResponse.Item responseItem = new TestRunResponse.Item();
            responseItem.setCaseId(caseId);
            responseItem.setName(name);
            responseItem.setInput(input);
            responseItem.setRenderedPrompt(renderResponse.getRenderedPrompt());
            responseItem.setModelOutput(modelOutput);
            responseItem.setSuccess(itemSuccess);
            responseItem.setError(error);
            responseItem.setLatencyMs(latency);
            items.add(responseItem);
            index++;
        }

        run.setTotalCases(cases.size());
        run.setSuccessCases(success);
        run.setFailedCases(failed);
        testRunRepository.save(run);

        TestRunResponse response = new TestRunResponse();
        response.setRunId(run.getId());
        response.setTotalCases(cases.size());
        response.setSuccessCases(success);
        response.setFailedCases(failed);
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
            item.setModelOutput(runItem.getModelOutput());
            item.setSuccess(runItem.isSuccess());
            item.setError(runItem.getErrorMessage());
            item.setLatencyMs(runItem.getLatencyMs());
            items.add(item);
        }
        response.setItems(items);
        return response;
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
}


