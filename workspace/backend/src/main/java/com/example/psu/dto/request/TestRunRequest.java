package com.example.psu.dto.request;

import lombok.Data;

import java.util.Map;

/**
 * 测试运行请求
 */
@Data
public class TestRunRequest {
    private Long compositionId;
    private Map<String, Object> options;
}
