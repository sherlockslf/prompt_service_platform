package com.example.psu.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评估报告响应
 */
@Data
public class EvaluationReportResponse {
    private Long id;
    private Long taskId;
    private BigDecimal overallScore;
    private BigDecimal passRate;
    private String summaryJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EvaluationItemResultResponse> issueItems = new ArrayList<>();
}

