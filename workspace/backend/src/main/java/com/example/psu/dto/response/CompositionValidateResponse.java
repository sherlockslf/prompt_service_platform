package com.example.psu.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 编排校验响应
 */
@Data
public class CompositionValidateResponse {
    private boolean ok;
    private List<Map<String, Object>> errors = new ArrayList<>();
    private List<Map<String, Object>> warnings = new ArrayList<>();
}
