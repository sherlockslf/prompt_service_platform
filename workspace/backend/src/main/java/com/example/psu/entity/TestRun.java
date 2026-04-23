package com.example.psu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试运行主表实体
 */
@Entity
@Table(
    name = "ai_prompt_test_runs",
    indexes = {
        @Index(name = "idx_test_runs_psu_id", columnList = "psuId"),
        @Index(name = "idx_test_runs_dataset_id", columnList = "datasetId")
    }
)
@Data
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false)
    private Long datasetId;

    @Column(nullable = false)
    private Long compositionId;

    private Integer compositionRevisionNo;

    @Column(nullable = false)
    private Integer totalCases = 0;

    @Column(nullable = false)
    private Integer successCases = 0;

    @Column(nullable = false)
    private Integer failedCases = 0;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
