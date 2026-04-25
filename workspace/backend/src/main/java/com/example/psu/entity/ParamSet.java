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
 * 参数集实体（覆盖写）
 */
@Entity
@Table(
    name = "ai_prompt_param_sets",
    indexes = {
        @Index(name = "idx_param_sets_psu_id", columnList = "psuId"),
        @Index(name = "idx_param_sets_modified_by", columnList = "modifiedBy"),
        @Index(name = "idx_param_sets_created", columnList = "createdAt")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_param_sets_psu_id", columnNames = {"psuId"})
    }
)
@Data
public class ParamSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false, columnDefinition = "JSON")
    private String paramSetContent;

    @Column(nullable = false)
    private Long modifiedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private String changeLog;

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
