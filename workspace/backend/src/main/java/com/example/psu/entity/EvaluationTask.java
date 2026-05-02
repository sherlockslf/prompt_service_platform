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
 * 评估任务实体
 *
 * @author SLF
 * @date 2026-04-29
 * @description 保存评估任务主状态与汇总指标
 */
@Entity
@Table(
    name = "ai_prompt_evaluation_tasks",
    indexes = {
        @Index(name = "idx_eval_tasks_psu_id", columnList = "psuId"),
        @Index(name = "idx_eval_tasks_dataset_id", columnList = "datasetId"),
        @Index(name = "idx_eval_tasks_created_at", columnList = "createdAt")
    }
)
@Data
public class EvaluationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false)
    private Long datasetId;

    @Column(nullable = false, length = 32)
    private String status = "CREATED";

    @Column(nullable = false)
    private Integer totalCases = 0;

    @Column(nullable = false)
    private Integer processedCases = 0;

    @Column(nullable = false)
    private Integer successCases = 0;

    @Column(nullable = false)
    private Integer failedCases = 0;

    private BigDecimal averageScore;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Long createdBy = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        // 保留空实现，确保JPA更新时序可扩展。
    }
}

