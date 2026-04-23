package com.example.psu.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 编排渲染响应
 */
@Data
public class CompositionRenderResponse {
    private String renderedPrompt;
    private List<String> missingVars;
    private List<String> usedVars;
}
