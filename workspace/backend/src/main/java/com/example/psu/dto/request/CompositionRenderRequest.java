package com.example.psu.dto.request;

import lombok.Data;

import java.util.Map;

/**
 * 编排渲染请求
 */
@Data
public class CompositionRenderRequest {
    private Long compositionId;
    private Map<String, Object> input;
}
