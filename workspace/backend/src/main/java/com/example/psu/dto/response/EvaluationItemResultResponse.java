package com.example.psu.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 评估明细响应
 */
@Data
public class EvaluationItemResultResponse {
    private Long id;
    private String caseId;
    private String caseName;
    private Map<String, Object> input;
    private String renderedPrompt;
    private String actualOutput;
    private String status;
    private BigDecimal relevanceScore;
    private BigDecimal completenessScore;
    private BigDecimal formatScore;
    private BigDecimal totalScore;
    private String reason;
}

