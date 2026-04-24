package com.example.psu.dto.response;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 对外解析响应
 */
@Data
public class ResolvePromptResponse {
    private Long psuId;
    private String environment;
    private Long releaseId;
    private Integer revisionNo;
    private String routeType;
    private Long ruleId;
    private Map<String, Object> renderConfig = new HashMap<>();
}
