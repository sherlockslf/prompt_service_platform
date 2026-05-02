package com.example.psu.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评估任务响应
 */
@Data
public class EvaluationTaskResponse {
    private Long id;
    private Long psuId;
    private Long datasetId;
    private String status;
    private Integer totalCases;
    private Integer processedCases;
    private Integer successCases;
    private Integer failedCases;
    private BigDecimal averageScore;
    private String errorMessage;
    private Long reportId;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<EvaluationItemResultResponse> items = new ArrayList<>();
}

