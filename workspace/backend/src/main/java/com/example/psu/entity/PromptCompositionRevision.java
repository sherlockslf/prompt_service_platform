package com.example.psu.entity;

import com.example.psu.enums.CompositionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 编排快照实体
 */
@Entity
@Table(
    name = "ai_prompt_composition_revisions",
    indexes = {
        @Index(name = "idx_comp_rev_psu_id", columnList = "psuId")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_comp_rev", columnNames = {"compositionId", "revisionNo"})
    }
)
@Data
public class PromptCompositionRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long compositionId;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false)
    private Integer revisionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompositionStatus statusAtTime;

    @Column(nullable = false)
    private Integer schemaVersionAtTime;

    @Column(nullable = false)
    private Integer schemaVersion;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String contentSnapshot;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String specJsonSnapshot;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
