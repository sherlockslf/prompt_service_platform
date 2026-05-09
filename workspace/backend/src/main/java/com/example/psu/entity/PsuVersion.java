package com.example.psu.entity;

import com.example.psu.enums.PsuStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PSU版本历史快照
 */
@Entity
@Table(
    name = "ai_prompt_psu_versions",
    indexes = {
        @Index(name = "idx_psu_versions_psu_id", columnList = "psuId"),
        @Index(name = "idx_psu_versions_version_no", columnList = "versionNo"),
        @Index(name = "idx_psu_versions_created_at", columnList = "createdAt")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_psu_versions_psu_version", columnNames = {"psuId", "versionNo"})
    }
)
@Data
public class PsuVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuRefId;

    @Column(nullable = false)
    private String psuId;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PsuStatus status;

    @Column(nullable = false)
    private Long operatorId;

    @Column(nullable = false)
    private String changeSource;

    private Integer schemaVersionNo;

    private Long compositionId;

    private Integer compositionRevisionNo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
