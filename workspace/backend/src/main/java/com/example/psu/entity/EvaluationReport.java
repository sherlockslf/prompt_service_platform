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
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评估报告实体
 *
 * @author SLF
 * @date 2026-04-29
 * @description 保存任务级汇总报告与摘要JSON
 */
@Entity
@Table(
    name = "ai_prompt_evaluation_reports",
    indexes = {
        @Index(name = "idx_eval_reports_created_at", columnList = "createdAt")
    }
)
@Data
public class EvaluationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long taskId;

    private BigDecimal overallScore;
    private BigDecimal passRate;

    @Column(columnDefinition = "LONGTEXT")
    private String summaryJson;

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

