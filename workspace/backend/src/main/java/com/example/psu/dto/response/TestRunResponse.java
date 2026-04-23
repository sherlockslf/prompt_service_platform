package com.example.psu.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 测试运行响应
 */
@Data
public class TestRunResponse {
    private Long runId;
    private Integer totalCases;
    private Integer successCases;
    private Integer failedCases;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private String caseId;
        private String name;
        private Map<String, Object> input;
        private String renderedPrompt;
        private String modelOutput;
        private boolean success;
        private String error;
        private Integer latencyMs;
    }
}
