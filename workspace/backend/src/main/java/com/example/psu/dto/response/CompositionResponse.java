package com.example.psu.dto.response;

import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.RejectionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 编排草稿响应
 */
@Data
public class CompositionResponse {
    private Long id;
    private Long psuId;
    private CompositionStatus status;
    private Integer schemaVersion;
    private String content;
    private Map<String, Object> specJson;
    private String rejectionReason;
    private RejectionType rejectionType;
    private LocalDateTime updatedAt;
}
