package com.example.psu.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参数集响应
 */
@Data
public class ParamSetResponse {
    private Long id;
    private Long psuId;
    private String paramSetContent;
    private Long modifiedBy;
    private String modifierName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String changeLog;
}
