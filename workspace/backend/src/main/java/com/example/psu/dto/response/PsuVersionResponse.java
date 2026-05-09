package com.example.psu.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PsuVersionResponse {
    private Long id;
    private Long psuRefId;
    private String psuId;
    private Integer versionNo;
    private String name;
    private String description;
    private String status;
    private Long operatorId;
    private String changeSource;
    private Integer schemaVersionNo;
    private Long compositionId;
    private Integer compositionRevisionNo;
    private LocalDateTime createdAt;
}
