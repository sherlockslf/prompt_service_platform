package com.example.psu.dto.request;

import com.example.psu.enums.ReleaseRuleType;
import com.example.psu.enums.RuleOperator;
import lombok.Data;

/**
 * 发布规则请求
 */
@Data
public class ReleaseRuleRequest {
    private ReleaseRuleType ruleType;
    private String ruleKey;
    private RuleOperator operator;
    private String ruleValue;
    private Integer trafficPercent;
    private Integer priority;
    private Boolean enabled;
}
