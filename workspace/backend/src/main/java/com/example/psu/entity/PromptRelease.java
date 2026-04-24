package com.example.psu.entity;

import com.example.psu.enums.ReleaseStatus;
import com.example.psu.enums.ReleaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布单实体
 */
@Entity
@Table(
    name = "ai_prompt_releases",
    indexes = {
        @Index(name = "idx_release_psu_env_status", columnList = "psuId,environment,status"),
        @Index(name = "idx_release_target", columnList = "targetCompositionId,targetRevisionNo")
    }
)
@Data
public class PromptRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false, length = 32)
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReleaseType releaseType;

    @Column(nullable = false)
    private Long targetCompositionId;

    @Column(nullable = false)
    private Integer targetRevisionNo;

    private Integer baseRevisionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReleaseStatus status = ReleaseStatus.DRAFT;

    private Long approvalBy;
    private LocalDateTime approvedAt;
    private Long executedBy;
    private LocalDateTime executedAt;
    private Integer rollbackToRevisionNo;

    @Column(length = 500)
    private String rollbackReason;

    @Column(nullable = false)
    private Long createdBy = 0L;

    @Column(nullable = false)
    private Long updatedBy = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
