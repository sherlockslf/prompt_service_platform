package com.example.psu.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 创建评估任务请求
 */
@Data
public class CreateEvaluationTaskRequest {
    private Long psuId;
    private Long datasetId;
    private List<String> dimensions;
}

