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
 * 回滚记录实体
 */
@Entity
@Table(
    name = "ai_prompt_rollbacks",
    indexes = {
        @Index(name = "idx_rollbacks_psu_env", columnList = "psuId,environment"),
        @Index(name = "idx_rollbacks_created_at", columnList = "createdAt")
    }
)
@Data
public class PromptRollbackRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(nullable = false)
    private Long fromReleaseId;

    @Column(nullable = false)
    private Integer fromRevisionNo;

    @Column(nullable = false)
    private Long toReleaseId;

    @Column(nullable = false)
    private Integer toRevisionNo;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private Long operatorId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
