package com.example.psu.entity;

import com.example.psu.enums.RejectionType;
import com.example.psu.enums.ReviewStatus;
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
 * 版本审核记录实体
 */
@Entity
@Table(
    name = "ai_prompt_version_reviews",
    indexes = {
        @Index(name = "idx_version_reviews_psu_id", columnList = "psuId"),
        @Index(name = "idx_version_reviews_status", columnList = "status"),
        @Index(name = "idx_version_reviews_submitter", columnList = "submitterId"),
        @Index(name = "idx_version_reviews_reviewer", columnList = "reviewerId"),
        @Index(name = "idx_version_reviews_submitted", columnList = "submittedAt"),
        @Index(name = "idx_version_reviews_git_hash", columnList = "gitCommitHash")
    }
)
@Data
public class VersionReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long psuId;

    @Column(nullable = false)
    private Integer versionNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.DRAFT;

    @Column(nullable = false)
    private Long submitterId;

    private Long reviewerId;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private RejectionType rejectionType;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @Column(length = 64)
    private String gitCommitHash;

    @Column(columnDefinition = "LONGTEXT")
    private String codeContent;

    private Long compositionId;

    private Integer compositionRevisionNo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.submittedAt == null) {
            this.submittedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
