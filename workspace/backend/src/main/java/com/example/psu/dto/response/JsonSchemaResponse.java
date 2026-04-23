package com.example.psu.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * JSON Schema响应DTO
 */
@Data
public class JsonSchemaResponse {
    private Long id;
    private Long psuId;
    private String schemaContent;
    private Integer version;
    private Long modifiedBy;
    private String modifierName;
    private LocalDateTime createdAt;
    private String changeLog;
}
