package com.example.psu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境生效版本指针实体
 */
@Entity
@Table(
    name = "ai_prompt_live_versions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_live_psu_env", columnNames = {"psuId", "environment"})
    },
    indexes = {
        @Index(name = "idx_live_psu_env", columnList = "psuId,environment")
    }
)
@Data
public class PromptLiveVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(nullable = false)
    private Long stableReleaseId;

    @Column(nullable = false)
    private Integer stableRevisionNo;

    private Long canaryReleaseId;

    @Column(nullable = false)
    private Long updatedBy = 0L;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
