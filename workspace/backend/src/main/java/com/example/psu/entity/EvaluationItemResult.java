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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评估明细结果实体
 *
 * @author SLF
 * @date 2026-04-29
 * @description 保存每条用例的评分与失败原因
 */
@Entity
@Table(
    name = "ai_prompt_evaluation_item_results",
    indexes = {
        @Index(name = "idx_eval_items_task_id", columnList = "taskId"),
        @Index(name = "idx_eval_items_case_id", columnList = "caseId"),
        @Index(name = "idx_eval_items_created_at", columnList = "createdAt")
    }
)
@Data
public class EvaluationItemResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String caseName;

    @Column(columnDefinition = "LONGTEXT")
    private String inputJson;

    @Column(columnDefinition = "LONGTEXT")
    private String renderedPrompt;

    @Column(columnDefinition = "LONGTEXT")
    private String actualOutput;

    @Column(nullable = false, length = 32)
    private String status = "SUCCESS";

    private BigDecimal relevanceScore;
    private BigDecimal completenessScore;
    private BigDecimal formatScore;
    private BigDecimal totalScore;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

