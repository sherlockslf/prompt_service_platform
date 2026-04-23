package com.example.psu.entity;

import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.RejectionType;
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
 * 编排草稿实体
 */
@Entity
@Table(
    name = "ai_prompt_compositions",
    indexes = {
        @Index(name = "idx_compositions_psu_id", columnList = "psuId"),
        @Index(name = "idx_compositions_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_compositions_psu_id", columnNames = {"psuId"})
    }
)
@Data
public class PromptComposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false)
    private Integer schemaVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompositionStatus status = CompositionStatus.DRAFT;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(columnDefinition = "LONGTEXT")
    private String specJson;

    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private RejectionType rejectionType;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private Long updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
