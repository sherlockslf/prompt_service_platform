package com.example.psu.entity;

import com.example.psu.enums.ReleaseRuleType;
import com.example.psu.enums.RuleOperator;
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
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布规则实体
 */
@Entity
@Table(
    name = "ai_prompt_release_rules",
    indexes = {
        @Index(name = "idx_rules_release", columnList = "releaseId,enabled,priority")
    }
)
@Data
public class PromptReleaseRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long releaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReleaseRuleType ruleType;

    @Column(length = 64)
    private String ruleKey;

    @Enumerated(EnumType.STRING)
    private RuleOperator operator;

    @Column(columnDefinition = "TEXT")
    private String ruleValue;

    private Integer trafficPercent;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
