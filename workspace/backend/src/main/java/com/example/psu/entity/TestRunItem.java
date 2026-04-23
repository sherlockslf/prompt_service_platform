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
 * 测试运行明细实体
 */
@Entity
@Table(
    name = "ai_prompt_test_run_items",
    indexes = {
        @Index(name = "idx_test_run_items_run_id", columnList = "runId"),
        @Index(name = "idx_test_run_items_case_id", columnList = "caseId")
    }
)
@Data
public class TestRunItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long runId;

    @Column(nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String caseName;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String inputJson;

    @Column(columnDefinition = "LONGTEXT")
    private String renderedPrompt;

    @Column(columnDefinition = "LONGTEXT")
    private String modelOutput;

    @Column(nullable = false)
    private boolean success = true;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer latencyMs;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
