package com.example.psu.entity;

import com.example.psu.enums.PsuTag;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核发布记录（与编辑流程解耦）
 */
@Entity
@Table(
    name = "ai_prompt_release_versions",
    indexes = {
        @Index(name = "idx_release_versions_psu_id", columnList = "psuId"),
        @Index(name = "idx_release_versions_psu_ver", columnList = "psuId,psuVersionNo"),
        @Index(name = "idx_release_versions_tag", columnList = "tag")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_release_versions_psu_ver", columnNames = {"psuId", "psuVersionNo"})
    }
)
@Data
public class PsuReleaseVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false)
    private Integer psuVersionNo;

    @Column(nullable = false)
    private Long jsonSchemaId;

    @Column(nullable = false)
    private Integer jsonSchemaVersionNo;

    @Column(nullable = false)
    private Long promptId;

    @Column(nullable = false)
    private Integer promptVersionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PsuTag tag = PsuTag.PREVIEW;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
