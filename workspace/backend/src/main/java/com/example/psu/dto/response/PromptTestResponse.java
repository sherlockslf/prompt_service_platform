package com.example.psu.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Prompt测试响应
 *
 * @author SLF
 * @date 2026-04-29
 * @description 统一返回Prompt测试结果，便于前端按固定结构展示
 */
@Data
public class PromptTestResponse {
    private String renderedPrompt;
    private List<String> missingVars;
    private String modelOutput;
    private String provider;
    private String model;
    private Integer latencyMs;
    private String traceId;
}
