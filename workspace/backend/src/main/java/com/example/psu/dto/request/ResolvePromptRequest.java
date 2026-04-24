package com.example.psu.dto.request;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 对外解析请求
 */
@Data
public class ResolvePromptRequest {
    private Long psuId;
    private String environment;
    private Map<String, Object> context = new HashMap<>();
}
