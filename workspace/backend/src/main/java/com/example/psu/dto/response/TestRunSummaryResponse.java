package com.example.psu.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试运行摘要响应
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供测试运行历史列表展示所需的摘要字段
 */
@Data
public class TestRunSummaryResponse {
    private Long runId;
    private Long psuId;
    private Long datasetId;
    private Long compositionId;
    private Integer totalCases;
    private Integer successCases;
    private Integer failedCases;
    private String status;
    private String exceptionReason;
    private LocalDateTime createdAt;
}
