package com.example.psu.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * PSU响应DTO
 */
@Data
public class PsuResponse {
    private Long id;
    private String psuId;
    private String name;
    private String description;
    private String status;
    private Long creatorId;
    private String creatorName;
    private Integer versionNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
    