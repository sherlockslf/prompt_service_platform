package com.example.psu.service.impl;

import com.example.psu.dto.request.ReviewRequest;
import com.example.psu.dto.request.RollbackVersionRequest;
import com.example.psu.dto.response.VersionCompareResponse;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.entity.PsuUnit;
import com.example.psu.entity.VersionReview;
import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.PsuStatus;
import com.example.psu.enums.RejectionType;
import com.example.psu.enums.ReviewStatus;
import com.example.psu.exception.BusinessException;
import com.example.psu.exception.ErrorCode;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PromptCompositionRevisionRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 版本审核服务实现
 */
@Service
@Transactional
public class VersionReviewServiceImpl implements VersionReviewService {

    private final VersionReviewRepository versionReviewRepository;
    private final PsuRepository psuRepository;
    private final PromptCompositionRepository promptCompositionRepository;
    private final PromptCompositionRevisionRepository promptCompositionRevisionRepository;
    private final CompositionService compositionService;
    private final CodeGeneratorService codeGeneratorService;

    public VersionReviewServiceImpl(
        VersionReviewRepository versionReviewRepository,
        PsuRepository psuRepository,
        PromptCompositionRepository promptCompositionRepository,
        PromptCompositionRevisionRepository promptCompositionRevisionRepository,
        CompositionService compositionService,
        CodeGeneratorService codeGeneratorService
    ) {
        this.versionReviewRepository = versionReviewRepository;
        this.psuRepository = psuRepository;
        this.promptCompositionRepository = promptCompositionRepository;
        this.promptCompositionRevisionRepository = promptCompositionRevisionRepository;
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
        if (psu.getStatus() != PsuStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿状态允许提交审核");
        }

        PromptComposition composition = compositionService.getCompositionByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));

        if (composition.getStatus() == CompositionStatus.DRAFT) {
            composition = compositionService.submit(psuId, submitterId);
        } else if (composition.getStatus() != CompositionStatus.CANDIDATE) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_SUBMITTED, "当前编排状态不允许提交审核: " + composition.getStatus());
        }

        PromptCompositionRevision revision = compositionService.getLatestRevision(composition.getId()).orElse(null);
        if (revision == null) {
            revision = compositionService.createRevisionSnapshot(composition, submitterId);
        }

        boolean existsPending = versionReviewRepository.existsByCompositionIdAndCompositionRevisionNoAndStatus(
            composition.getId(),
            revision.getRevisionNo(),
            ReviewStatus.CANDIDATE
        );
        if (existsPending) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_SUBMITTED, "该编排版本已在审核中");
        }

        VersionReview review = new VersionReview();
        review.setPsuId(psuId);
        review.setSubmitterId(submitterId);
        review.setStatus(ReviewStatus.CANDIDATE);
        review.setVersionNo(psu.getVersionNo());
        review.setCompositionId(composition.getId());
        review.setCompositionRevisionNo(revision.getRevisionNo());
        review.setSubmittedAt(LocalDateTime.now());
        psu.setStatus(PsuStatus.CANDIDATE);
        psuRepository.save(psu);

        return versionReviewRepository.save(review);
    }

    @Override
    public VersionReview reviewVersion(Long reviewId, ReviewRequest request, Long reviewerId) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审核请求不能为空");
        }

        VersionReview review = versionReviewRepository.findById(reviewId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "审核记录不存在"));

        if (review.getStatus() != ReviewStatus.CANDIDATE) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_REVIEWED, "该审核记录已处理");
        }

        review.setReviewerId(reviewerId);
        review.setReviewedAt(LocalDateTime.now());

        if (request.isApproved()) {
            review.setStatus(ReviewStatus.FORMAL);
            review.setRejectionReason(null);
            review.setRejectionType(null);
            if (review.getCodeContent() == null || review.getCodeContent().isBlank()) {
                review.setCodeContent(codeGeneratorService.generateCompleteBusinessCode(review.getPsuId()));
            }
            compositionService.updateStatus(review.getPsuId(), CompositionStatus.FORMAL, null, null);
            PsuUnit psu = psuRepository.findById(review.getPsuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在"));
            psu.setStatus(PsuStatus.FORMAL);
            psuRepository.save(psu);
            archiveOlderFormalReviews(review);
        } else {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回时必须填写rejectionReason");
            }
            if (request.getRejectionType() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回时必须填写rejectionType");
            }

            review.setStatus(ReviewStatus.ARCHIVED);
            review.setRejectionReason(request.getRejectionReason());
            review.setRejectionType(request.getRejectionType());

            PromptComposition draft = compositionService.updateStatus(
                review.getPsuId(),
                CompositionStatus.DRAFT,
                request.getRejectionReason(),
                request.getRejectionType()
            );
            compositionService.createRevisionSnapshot(draft, reviewerId);
            PsuUnit psu = psuRepository.findById(review.getPsuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在"));
            psu.setStatus(PsuStatus.DRAFT);
            psuRepository.save(psu);
        }

        return versionReviewRepository.save(review);
    }

    @Override
    public String getCode(Long psuId) {
        VersionReview approved = versionReviewRepository.findTopByPsuIdAndStatusOrderByReviewedAtDesc(psuId, ReviewStatus.FORMAL)
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

        if (review.getStatus() != ReviewStatus.FORMAL) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审核通过的记录允许登记Git提交");
        }

        review.setGitCommitHash(gitCommitHash.trim());
        review.setReviewerId(operatorId);
        review.setReviewedAt(LocalDateTime.now());
        return versionReviewRepository.save(review);
    }

    @Override
    public VersionCompareResponse compareVersions(Long psuId, Integer fromVersionNo, Integer toVersionNo) {
        VersionReview from = versionReviewRepository.findByPsuIdAndVersionNo(psuId, fromVersionNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "起始版本不存在"));
        VersionReview to = versionReviewRepository.findByPsuIdAndVersionNo(psuId, toVersionNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "目标版本不存在"));
        String fromPrompt = readPromptSnapshot(from);
        String toPrompt = readPromptSnapshot(to);

        VersionCompareResponse response = new VersionCompareResponse();
        response.setPsuId(psuId);
        response.setFromVersionNo(fromVersionNo);
        response.setToVersionNo(toVersionNo);
        response.setFromPrompt(fromPrompt);
        response.setToPrompt(toPrompt);
        fillDiffSummary(response, fromPrompt, toPrompt);
        return response;
    }

    @Override
    public VersionReview rollbackVersion(Long psuId, RollbackVersionRequest request, Long operatorId) {
        if (request == null || request.getTargetVersionNo() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "targetVersionNo不能为空");
        }
        PsuUnit psu = psuRepository.findById(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在"));
        if (psu.getStatus() != PsuStatus.FORMAL) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅正式版支持版本回滚");
        }

        VersionReview target = versionReviewRepository.findByPsuIdAndVersionNo(psuId, request.getTargetVersionNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "回滚目标版本不存在"));
        if (target.getStatus() != ReviewStatus.FORMAL && target.getStatus() != ReviewStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅允许回滚到历史正式版");
        }
        PromptCompositionRevision snapshot = promptCompositionRevisionRepository
            .findByCompositionIdAndRevisionNo(target.getCompositionId(), target.getCompositionRevisionNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "目标版本快照不存在"));
        PromptComposition composition = promptCompositionRepository.findByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));

        // 回滚时用目标快照覆盖当前编排，并记录新快照
        composition.setContent(snapshot.getContentSnapshot());
        composition.setSpecJson(snapshot.getSpecJsonSnapshot());
        composition.setStatus(CompositionStatus.FORMAL);
        composition.setUpdatedBy(operatorId == null ? 0L : operatorId);
        PromptComposition saved = promptCompositionRepository.save(composition);
        PromptCompositionRevision rollbackSnapshot = compositionService.createRevisionSnapshot(saved, operatorId);

        int rollbackVersionNo = (psu.getVersionNo() == null ? 0 : psu.getVersionNo()) + 1;
        VersionReview rollbackReview = new VersionReview();
        rollbackReview.setPsuId(psuId);
        rollbackReview.setVersionNo(rollbackVersionNo);
        rollbackReview.setStatus(ReviewStatus.FORMAL);
        rollbackReview.setSubmitterId(operatorId == null ? 0L : operatorId);
        rollbackReview.setReviewerId(operatorId == null ? 0L : operatorId);
        rollbackReview.setSubmittedAt(LocalDateTime.now());
        rollbackReview.setReviewedAt(LocalDateTime.now());
        rollbackReview.setCompositionId(saved.getId());
        rollbackReview.setCompositionRevisionNo(rollbackSnapshot.getRevisionNo());
        rollbackReview.setCodeContent(target.getCodeContent());
        rollbackReview.setRejectionReason(
            "rollback-to-v" + request.getTargetVersionNo() + (request.getReason() == null ? "" : (":" + request.getReason()))
        );

        VersionReview savedRollback = versionReviewRepository.save(rollbackReview);
        archiveOlderFormalReviews(savedRollback);
        psu.setVersionNo(rollbackVersionNo);
        psu.setStatus(PsuStatus.FORMAL);
        psuRepository.save(psu);
        return savedRollback;
    }

    private void archiveOlderFormalReviews(VersionReview currentFormal) {
        // 正式版唯一化：同一PSU只保留一个FORMAL，旧FORMAL转ARCHIVED
        List<VersionReview> all = versionReviewRepository.findByPsuIdOrderBySubmittedAtDesc(currentFormal.getPsuId());
        for (VersionReview item : all) {
            if (item.getId().equals(currentFormal.getId())) {
                continue;
            }
            if (item.getStatus() == ReviewStatus.FORMAL) {
                item.setStatus(ReviewStatus.ARCHIVED);
                versionReviewRepository.save(item);
            }
        }
    }

    private String readPromptSnapshot(VersionReview review) {
        if (review.getCompositionId() == null || review.getCompositionRevisionNo() == null) {
            return "";
        }
        return promptCompositionRevisionRepository
            .findByCompositionIdAndRevisionNo(review.getCompositionId(), review.getCompositionRevisionNo())
            .map(PromptCompositionRevision::getContentSnapshot)
            .orElse("");
    }

    private void fillDiffSummary(VersionCompareResponse response, String fromPrompt, String toPrompt) {
        String[] fromLines = fromPrompt == null ? new String[0] : fromPrompt.split("\\R", -1);
        String[] toLines = toPrompt == null ? new String[0] : toPrompt.split("\\R", -1);
        Set<String> fromSet = new HashSet<>(List.of(fromLines));
        Set<String> toSet = new HashSet<>(List.of(toLines));
        int removed = 0;
        for (String line : fromSet) {
            if (!toSet.contains(line)) {
                removed++;
            }
        }
        int added = 0;
        for (String line : toSet) {
            if (!fromSet.contains(line)) {
                added++;
            }
        }
        response.setFromLineCount(fromLines.length);
        response.setToLineCount(toLines.length);
        response.setAddedLineCount(added);
        response.setRemovedLineCount(removed);
        response.setChanged(!fromPrompt.equals(toPrompt));
    }
}
