package com.example.psu.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Prompt片段响应DTO
 */
@Data
public class PromptFragmentResponse {
    private Long id;
    private Long psuId;
    private String fragmentKey;
    private String content;
    private Boolean editable;
    private String type;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
