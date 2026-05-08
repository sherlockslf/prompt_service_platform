package com.example.psu.dto.response;

import lombok.Data;

/**
 * 对外查询PSU指定标签的Prompt与Schema响应
 */
@Data
public class PromptSchemaResolveResponse {
    private String psuId;
    private String tag;
    private Long promptId;
    private Integer promptVersionNo;
    private String prompt;
    private Long jsonSchemaId;
    private Integer jsonSchemaVersionNo;
    private String jsonSchema;
}
