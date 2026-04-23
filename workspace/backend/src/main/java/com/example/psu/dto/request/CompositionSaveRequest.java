package com.example.psu.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 编排草稿保存请求
 */
@Data
public class CompositionSaveRequest {
    private String content;
    private List<Map<String, Object>> tokens;
    private List<Map<String, Object>> injectionPlan;
    private List<Map<String, Object>> assembledFragments;
    private Map<String, Object> specJson;
}
