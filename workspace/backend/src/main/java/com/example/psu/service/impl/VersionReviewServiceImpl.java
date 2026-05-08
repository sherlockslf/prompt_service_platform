package com.example.psu.service.impl;

import com.example.psu.dto.request.ReviewRequest;
import com.example.psu.dto.request.RollbackVersionRequest;
import com.example.psu.dto.response.VersionCompareResponse;
import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.entity.PsuReleaseVersion;
import com.example.psu.entity.PsuUnit;
import com.example.psu.entity.VersionReview;
import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.PsuStatus;
import com.example.psu.enums.PsuTag;
import com.example.psu.enums.ReviewStatus;
import com.example.psu.exception.BusinessException;
import com.example.psu.exception.ErrorCode;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PromptCompositionRevisionRepository;
import com.example.psu.repository.PsuReleaseVersionRepository;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional
@SuppressWarnings("null")
public class VersionReviewServiceImpl implements VersionReviewService {
    private static final Pattern GIT_COMMIT_HASH_PATTERN = Pattern.compile("^[0-9a-fA-F]{7,40}$");
    private static final String LANGUAGE_JAVA = "java";
    private static final String LANGUAGE_PYTHON = "python";

    private final VersionReviewRepository versionReviewRepository;
    private final PsuRepository psuRepository;
    private final PromptCompositionRepository promptCompositionRepository;
    private final PromptCompositionRevisionRepository promptCompositionRevisionRepository;
    private final JsonSchemaRepository jsonSchemaRepository;
    private final PsuReleaseVersionRepository psuReleaseVersionRepository;
    private final CompositionService compositionService;
    private final CodeGeneratorService codeGeneratorService;

    public VersionReviewServiceImpl(
        VersionReviewRepository versionReviewRepository,
        PsuRepository psuRepository,
        PromptCompositionRepository promptCompositionRepository,
        PromptCompositionRevisionRepository promptCompositionRevisionRepository,
        JsonSchemaRepository jsonSchemaRepository,
        PsuReleaseVersionRepository psuReleaseVersionRepository,
        CompositionService compositionService,
        CodeGeneratorService codeGeneratorService
    ) {
        this.versionReviewRepository = versionReviewRepository;
        this.psuRepository = psuRepository;
        this.promptCompositionRepository = promptCompositionRepository;
        this.promptCompositionRevisionRepository = promptCompositionRevisionRepository;
        this.jsonSchemaRepository = jsonSchemaRepository;
        this.psuReleaseVersionRepository = psuReleaseVersionRepository;
        this.compositionService = compositionService;
        this.codeGeneratorService = codeGeneratorService;
    }

    @Override
    public Page<VersionReview> getVersionReviews(Long psuId, Pageable pageable) {
        RequestValidationUtils.requireNonNull(pageable, "pageable");
        if (psuId == null) {
            return versionReviewRepository.findAll(pageable);
        }
        return versionReviewRepository.findByPsuId(psuId, pageable);
    }

    @Override
    public Optional<VersionReview> getVersionReviewById(Long id) {
        RequestValidationUtils.requireNonNull(id, "id");
        return versionReviewRepository.findById(id);
    }

    @Override
    public VersionReview submitVersion(Long psuId, Long submitterId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonNull(submitterId, "submitterId");
        PsuUnit psu = psuRepository.findById(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在: " + psuId));
        if (psu.getStatus() == PsuStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已删除PSU不允许提交审核");
        }
        if (versionReviewRepository.findByPsuIdAndVersionNo(psuId, psu.getVersionNo()).isPresent()) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_SUBMITTED, "当前PSU版本已提交审核");
        }

        // 获取编排，如果状态不是CANDIDATE则提交审核使其变为CANDIDATE
        PromptComposition composition = compositionService.getCompositionByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));
        if (composition.getStatus() != CompositionStatus.CANDIDATE) {
            composition = compositionService.submit(psuId, submitterId);
        }

        PromptCompositionRevision revision = compositionService.getLatestRevision(composition.getId()).orElse(null);
        if (revision == null) {
            revision = compositionService.createRevisionSnapshot(composition, submitterId);
        }

        VersionReview review = new VersionReview();
        review.setPsuId(psuId);
        review.setSubmitterId(submitterId);
        review.setStatus(ReviewStatus.CANDIDATE);
        review.setVersionNo(psu.getVersionNo());
        review.setCompositionId(composition.getId());
        review.setCompositionRevisionNo(revision.getRevisionNo());
        review.setSubmittedAt(LocalDateTime.now());

        VersionReview saved = versionReviewRepository.save(review);
        upsertReleaseRecord(saved, PsuTag.PREVIEW);
        return saved;
    }

    @Override
    public VersionReview reviewVersion(Long reviewId, ReviewRequest request, Long reviewerId) {
        RequestValidationUtils.requireNonNull(reviewId, "reviewId");
        RequestValidationUtils.requireNonNull(reviewerId, "reviewerId");
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
            archiveOlderFormalReviews(review);
            upsertReleaseRecord(review, PsuTag.FORMAL);
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
            upsertReleaseRecord(review, PsuTag.PREVIEW);
        }
        return versionReviewRepository.save(review);
    }

    @Override
    public String getCode(Long psuId, String language) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        String normalizedLanguage = normalizeLanguage(language);
        PsuReleaseVersion formal = psuReleaseVersionRepository.findTopByPsuIdAndTagOrderByUpdatedAtDesc(psuId, PsuTag.FORMAL)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "未找到正式发布版本"));
        VersionReview approved = versionReviewRepository.findByPsuIdAndVersionNo(psuId, formal.getPsuVersionNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "正式版本审核记录不存在"));

        if (LANGUAGE_JAVA.equals(normalizedLanguage) && (approved.getCodeContent() == null || approved.getCodeContent().isBlank())) {
            approved.setCodeContent(codeGeneratorService.generateCompleteBusinessCode(psuId, LANGUAGE_JAVA));
            versionReviewRepository.save(approved);
        }
        if (LANGUAGE_PYTHON.equals(normalizedLanguage)) {
            return codeGeneratorService.generateCompleteBusinessCode(psuId, LANGUAGE_PYTHON);
        }
        return approved.getCodeContent();
    }

    @Override
    public VersionReview registerGitCommit(Long reviewId, String gitCommitHash, Long operatorId) {
        RequestValidationUtils.requireNonNull(reviewId, "reviewId");
        RequestValidationUtils.requireNonNull(operatorId, "operatorId");
        if (gitCommitHash == null || gitCommitHash.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "gitCommitHash不能为空");
        }
        String normalizedHash = gitCommitHash.trim();
        if (!GIT_COMMIT_HASH_PATTERN.matcher(normalizedHash).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "gitCommitHash格式非法，仅支持7-40位十六进制字符");
        }

        VersionReview review = versionReviewRepository.findById(reviewId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "审核记录不存在"));
        if (review.getStatus() != ReviewStatus.FORMAL) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审核通过记录允许登记Git提交");
        }
        review.setGitCommitHash(normalizedHash);
        review.setReviewerId(operatorId);
        review.setReviewedAt(LocalDateTime.now());
        return versionReviewRepository.save(review);
    }

    @Override
    public VersionCompareResponse compareVersions(Long psuId, Integer fromVersionNo, Integer toVersionNo) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonNull(fromVersionNo, "fromVersionNo");
        RequestValidationUtils.requireNonNull(toVersionNo, "toVersionNo");
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
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonNull(operatorId, "operatorId");
        if (request == null || request.getTargetVersionNo() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "targetVersionNo不能为空");
        }
        VersionReview target = versionReviewRepository.findByPsuIdAndVersionNo(psuId, request.getTargetVersionNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "回滚目标版本不存在"));
        PromptCompositionRevision snapshot = promptCompositionRevisionRepository
            .findByCompositionIdAndRevisionNo(target.getCompositionId(), target.getCompositionRevisionNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "目标版本快照不存在"));
        PromptComposition composition = promptCompositionRepository.findByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));

        composition.setContent(snapshot.getContentSnapshot());
        composition.setSpecJson(snapshot.getSpecJsonSnapshot());
        composition.setStatus(CompositionStatus.FORMAL);
        composition.setUpdatedBy(operatorId);
        promptCompositionRepository.save(composition);

        target.setStatus(ReviewStatus.FORMAL);
        target.setReviewedAt(LocalDateTime.now());
        target.setReviewerId(operatorId);
        target.setRejectionReason(
            "rollback-target-v" + request.getTargetVersionNo() + (request.getReason() == null ? "" : (":" + request.getReason()))
        );
        VersionReview savedTarget = versionReviewRepository.save(target);
        archiveOlderFormalReviews(savedTarget);
        upsertReleaseRecord(savedTarget, PsuTag.FORMAL);
        return savedTarget;
    }

    @Override
    public VersionReview assignVersionTag(Long reviewId, PsuTag tag, Long operatorId) {
        RequestValidationUtils.requireNonNull(reviewId, "reviewId");
        RequestValidationUtils.requireNonNull(tag, "tag");
        RequestValidationUtils.requireNonNull(operatorId, "operatorId");
        VersionReview review = versionReviewRepository.findById(reviewId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "审核记录不存在"));
        PsuReleaseVersion record = psuReleaseVersionRepository.findByPsuIdAndPsuVersionNo(review.getPsuId(), review.getVersionNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "发布记录不存在"));
        if (tag == PsuTag.FORMAL) {
            clearFormalTagInReleaseRecord(review.getPsuId(), record.getId());
        }
        record.setTag(tag);
        psuReleaseVersionRepository.save(record);
        review.setReviewerId(operatorId);
        review.setReviewedAt(LocalDateTime.now());
        return versionReviewRepository.save(review);
    }

    private void archiveOlderFormalReviews(VersionReview currentFormal) {
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

    private void upsertReleaseRecord(VersionReview review, PsuTag tag) {
        if (review.getPsuId() == null || review.getVersionNo() == null) {
            return;
        }
        Optional<PsuReleaseVersion> existing = psuReleaseVersionRepository
            .findByPsuIdAndPsuVersionNo(review.getPsuId(), review.getVersionNo())
            ;
        PsuReleaseVersion record = existing.orElseGet(PsuReleaseVersion::new);

        // 首次创建时固化审核/发布对象；后续仅允许更新tag，避免把已提审对象漂移到“最新草稿”。
        if (existing.isEmpty()) {
            record.setPsuId(review.getPsuId());
            record.setPsuVersionNo(review.getVersionNo());

            JsonSchema schema = jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(review.getPsuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Schema不存在，无法生成发布记录"));
            record.setJsonSchemaId(schema.getId());
            record.setJsonSchemaVersionNo(schema.getVersion() == null ? 1 : schema.getVersion());

            if (review.getCompositionId() == null || review.getCompositionRevisionNo() == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "编排快照不存在，无法生成发布记录");
            }
            record.setPromptId(review.getCompositionId());
            record.setPromptVersionNo(review.getCompositionRevisionNo());
        }

        record.setTag(tag == null ? PsuTag.PREVIEW : tag);
        if (record.getTag() == PsuTag.FORMAL) {
            clearFormalTagInReleaseRecord(review.getPsuId(), record.getId());
        }
        psuReleaseVersionRepository.save(record);
    }

    private void clearFormalTagInReleaseRecord(Long psuId, Long exceptId) {
        List<PsuReleaseVersion> formals = psuReleaseVersionRepository.findByPsuIdAndTag(psuId, PsuTag.FORMAL);
        for (PsuReleaseVersion item : formals) {
            if (exceptId != null && exceptId.equals(item.getId())) {
                continue;
            }
            item.setTag(PsuTag.PREVIEW);
            psuReleaseVersionRepository.save(item);
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
        response.setChanged(!Objects.equals(fromPrompt, toPrompt));
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return LANGUAGE_JAVA;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (LANGUAGE_PYTHON.equals(normalized)) {
            return LANGUAGE_PYTHON;
        }
        return LANGUAGE_JAVA;
    }
}
