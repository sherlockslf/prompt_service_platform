package com.example.psu.service.impl;

import com.example.psu.dto.request.ReviewRequest;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.entity.PsuUnit;
import com.example.psu.entity.VersionReview;
import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.RejectionType;
import com.example.psu.enums.ReviewStatus;
import com.example.psu.exception.BusinessException;
import com.example.psu.exception.ErrorCode;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.VersionReviewRepository;
import com.example.psu.service.CodeGeneratorService;
import com.example.psu.service.CompositionService;
import com.example.psu.service.VersionReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 版本审核服务实现
 */
@Service
@Transactional
public class VersionReviewServiceImpl implements VersionReviewService {

    private final VersionReviewRepository versionReviewRepository;
    private final PsuRepository psuRepository;
    private final CompositionService compositionService;
    private final CodeGeneratorService codeGeneratorService;

    public VersionReviewServiceImpl(
        VersionReviewRepository versionReviewRepository,
        PsuRepository psuRepository,
        CompositionService compositionService,
        CodeGeneratorService codeGeneratorService
    ) {
        this.versionReviewRepository = versionReviewRepository;
        this.psuRepository = psuRepository;
        this.compositionService = compositionService;
        this.codeGeneratorService = codeGeneratorService;
    }

    @Override
    public Page<VersionReview> getVersionReviews(Long psuId, Pageable pageable) {
        if (psuId == null) {
            return versionReviewRepository.findAll(pageable);
        }
        return versionReviewRepository.findByPsuId(psuId, pageable);
    }

    @Override
    public Optional<VersionReview> getVersionReviewById(Long id) {
        return versionReviewRepository.findById(id);
    }

    @Override
    public VersionReview submitVersion(Long psuId, Long submitterId) {
        PsuUnit psu = psuRepository.findById(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在: " + psuId));

        PromptComposition composition = compositionService.getCompositionByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));

        if (composition.getStatus() == CompositionStatus.DRAFT) {
            composition = compositionService.submit(psuId, submitterId);
        } else if (composition.getStatus() != CompositionStatus.SUBMITTED && composition.getStatus() != CompositionStatus.DEV_REVIEWING) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_SUBMITTED, "当前编排状态不允许提交审核: " + composition.getStatus());
        }

        PromptCompositionRevision revision = compositionService.getLatestRevision(composition.getId()).orElse(null);
        if (revision == null) {
            revision = compositionService.createRevisionSnapshot(composition, submitterId);
        }

        boolean existsPending = versionReviewRepository.existsByCompositionIdAndCompositionRevisionNoAndStatus(
            composition.getId(),
            revision.getRevisionNo(),
            ReviewStatus.PENDING
        );
        if (existsPending) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_SUBMITTED, "该编排版本已在审核中");
        }

        VersionReview review = new VersionReview();
        review.setPsuId(psuId);
        review.setSubmitterId(submitterId);
        review.setStatus(ReviewStatus.PENDING);
        review.setMajorVersion(psu.getMajorVersion());
        review.setMinorVersion(psu.getMinorVersion());
        review.setPatchVersion(psu.getPatchVersion());
        review.setCompositionId(composition.getId());
        review.setCompositionRevisionNo(revision.getRevisionNo());
        review.setSubmittedAt(LocalDateTime.now());

        return versionReviewRepository.save(review);
    }

    @Override
    public VersionReview reviewVersion(Long reviewId, ReviewRequest request, Long reviewerId) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审核请求不能为空");
        }

        VersionReview review = versionReviewRepository.findById(reviewId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "审核记录不存在"));

        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_REVIEWED, "该审核记录已处理");
        }

        review.setReviewerId(reviewerId);
        review.setReviewedAt(LocalDateTime.now());

        if (request.isApproved()) {
            review.setStatus(ReviewStatus.APPROVED);
            review.setRejectionReason(null);
            review.setRejectionType(null);
            if (review.getCodeContent() == null || review.getCodeContent().isBlank()) {
                review.setCodeContent(codeGeneratorService.generateCompleteBusinessCode(review.getPsuId()));
            }
            compositionService.updateStatus(review.getPsuId(), CompositionStatus.APPROVED, null, null);
        } else {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回时必须填写rejectionReason");
            }
            if (request.getRejectionType() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回时必须填写rejectionType");
            }

            review.setStatus(ReviewStatus.REJECTED);
            review.setRejectionReason(request.getRejectionReason());
            review.setRejectionType(request.getRejectionType());

            if (request.getRejectionType() == RejectionType.BACK_TO_BIZ) {
                PromptComposition draft = compositionService.updateStatus(
                    review.getPsuId(),
                    CompositionStatus.DRAFT,
                    request.getRejectionReason(),
                    request.getRejectionType()
                );
                compositionService.createRevisionSnapshot(draft, reviewerId);
            } else {
                compositionService.updateStatus(
                    review.getPsuId(),
                    CompositionStatus.REJECTED,
                    request.getRejectionReason(),
                    request.getRejectionType()
                );
            }
        }

        return versionReviewRepository.save(review);
    }

    @Override
    public String getCode(Long psuId) {
        VersionReview approved = versionReviewRepository.findTopByPsuIdAndStatusOrderByReviewedAtDesc(psuId, ReviewStatus.APPROVED)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "未找到已通过审核的版本"));

        if (approved.getCodeContent() == null || approved.getCodeContent().isBlank()) {
            approved.setCodeContent(codeGeneratorService.generateCompleteBusinessCode(psuId));
            versionReviewRepository.save(approved);
        }

        return approved.getCodeContent();
    }

    @Override
    public VersionReview registerGitCommit(Long reviewId, String gitCommitHash, Long operatorId) {
        if (gitCommitHash == null || gitCommitHash.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "gitCommitHash不能为空");
        }

        VersionReview review = versionReviewRepository.findById(reviewId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "审核记录不存在"));

        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审核通过的记录允许登记Git提交");
        }

        review.setGitCommitHash(gitCommitHash.trim());
        review.setReviewerId(operatorId);
        review.setReviewedAt(LocalDateTime.now());
        return versionReviewRepository.save(review);
    }
}
