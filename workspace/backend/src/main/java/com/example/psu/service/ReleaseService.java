package com.example.psu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.psu.dto.request.CreateReleaseRequest;
import com.example.psu.dto.request.ReleaseRuleRequest;
import com.example.psu.dto.request.ResolvePromptRequest;
import com.example.psu.dto.request.ReviewReleaseRequest;
import com.example.psu.dto.request.RollbackReleaseRequest;
import com.example.psu.dto.response.PromptSchemaResolveResponse;
import com.example.psu.dto.response.ResolvePromptResponse;
import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.entity.PromptLiveVersion;
import com.example.psu.entity.PromptRelease;
import com.example.psu.entity.PromptReleaseRule;
import com.example.psu.entity.PromptRollbackRecord;
import com.example.psu.entity.PsuReleaseVersion;
import com.example.psu.entity.PsuUnit;
import com.example.psu.enums.PsuTag;
import com.example.psu.enums.ReleaseRuleType;
import com.example.psu.enums.ReleaseStatus;
import com.example.psu.enums.ReleaseType;
import com.example.psu.enums.RuleOperator;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.PromptCompositionRevisionRepository;
import com.example.psu.repository.PromptLiveVersionRepository;
import com.example.psu.repository.PromptReleaseRepository;
import com.example.psu.repository.PromptReleaseRuleRepository;
import com.example.psu.repository.PromptRollbackRecordRepository;
import com.example.psu.repository.PsuReleaseVersionRepository;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.JsonSchemaRepository;

/**
 * 发布域服务
 */
@Service
@Transactional
public class ReleaseService {

    private final PromptReleaseRepository promptReleaseRepository;
    private final PromptReleaseRuleRepository promptReleaseRuleRepository;
    private final PromptLiveVersionRepository promptLiveVersionRepository;
    private final PromptRollbackRecordRepository promptRollbackRecordRepository;
    private final PromptCompositionRevisionRepository promptCompositionRevisionRepository;
    private final JsonSchemaRepository jsonSchemaRepository;
    private final PsuRepository psuRepository;
    private final PsuReleaseVersionRepository psuReleaseVersionRepository;

    public ReleaseService(
        PromptReleaseRepository promptReleaseRepository,
        PromptReleaseRuleRepository promptReleaseRuleRepository,
        PromptLiveVersionRepository promptLiveVersionRepository,
        PromptRollbackRecordRepository promptRollbackRecordRepository,
        PromptCompositionRevisionRepository promptCompositionRevisionRepository,
        JsonSchemaRepository jsonSchemaRepository,
        PsuRepository psuRepository,
        PsuReleaseVersionRepository psuReleaseVersionRepository
    ) {
        this.promptReleaseRepository = promptReleaseRepository;
        this.promptReleaseRuleRepository = promptReleaseRuleRepository;
        this.promptLiveVersionRepository = promptLiveVersionRepository;
        this.promptRollbackRecordRepository = promptRollbackRecordRepository;
        this.promptCompositionRevisionRepository = promptCompositionRevisionRepository;
        this.jsonSchemaRepository = jsonSchemaRepository;
        this.psuRepository = psuRepository;
        this.psuReleaseVersionRepository = psuReleaseVersionRepository;
    }

    public Page<PromptRelease> getReleases(Long psuId, String environment, Pageable pageable) {
        RequestValidationUtils.requireNonNull(pageable, "pageable");
        Pageable safePageable = Objects.requireNonNull(pageable);
        if (psuId == null) {
            return promptReleaseRepository.findAll(safePageable);
        }
        Long safePsuId = Objects.requireNonNull(psuId);
        if (environment != null && !environment.isBlank()) {
            return promptReleaseRepository.findByPsuIdAndEnvironment(safePsuId, environment, safePageable);
        }
        return promptReleaseRepository.findByPsuId(safePsuId, safePageable);
    }

    public PromptRelease getRelease(Long releaseId) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        Long safeReleaseId = Objects.requireNonNull(releaseId);
        return promptReleaseRepository.findById(safeReleaseId)
            .orElseThrow(() -> new IllegalArgumentException("发布单不存在: " + safeReleaseId));
    }

    public PromptRelease createRelease(CreateReleaseRequest request, Long operatorId) {
        RequestValidationUtils.requireNonNull(request, "request");
        validateCreateRequest(request);
        Long safePsuId = Objects.requireNonNull(request.getPsuId());
        // 确认PSU存在
        psuRepository.findById(safePsuId)
            .orElseThrow(() -> new IllegalArgumentException("PSU不存在: " + safePsuId));
        psuReleaseVersionRepository.findTopByPsuIdAndTagOrderByUpdatedAtDesc(safePsuId, PsuTag.FORMAL)
            .orElseThrow(() -> new IllegalArgumentException("未找到正式版本，不允许创建发布单"));
        promptCompositionRevisionRepository
            .findByCompositionIdAndRevisionNo(request.getTargetCompositionId(), request.getTargetRevisionNo())
            .orElseThrow(() -> new IllegalArgumentException("目标快照不存在"));
        boolean approvedTarget = psuReleaseVersionRepository.findByPsuIdAndPromptIdAndPromptVersionNoAndTag(
            safePsuId,
            request.getTargetCompositionId(),
            request.getTargetRevisionNo(),
            PsuTag.FORMAL
        ).isPresent();
        if (!approvedTarget) {
            throw new IllegalArgumentException("目标版本未审核通过，不允许发布");
        }

        PromptRelease release = new PromptRelease();
        release.setPsuId(safePsuId);
        release.setEnvironment(request.getEnvironment().trim().toUpperCase());
        release.setReleaseType(request.getReleaseType());
        release.setTargetCompositionId(request.getTargetCompositionId());
        release.setTargetRevisionNo(request.getTargetRevisionNo());
        release.setBaseRevisionNo(request.getBaseRevisionNo());
        release.setStatus(ReleaseStatus.DRAFT);
        release.setCreatedBy(operatorId);
        release.setUpdatedBy(operatorId);
        return promptReleaseRepository.save(release);
    }

    public PromptRelease submit(Long releaseId, Long operatorId) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        PromptRelease release = getRelease(releaseId);
        if (release.getStatus() != ReleaseStatus.DRAFT) {
            throw new IllegalArgumentException("仅DRAFT状态可提交审核");
        }
        release.setStatus(ReleaseStatus.PENDING_APPROVAL);
        release.setUpdatedBy(operatorId);
        return promptReleaseRepository.save(release);
    }

    public PromptRelease approve(Long releaseId, Long operatorId) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        PromptRelease release = getRelease(releaseId);
        if (release.getStatus() != ReleaseStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("仅待审核状态可通过");
        }
        release.setStatus(ReleaseStatus.APPROVED);
        release.setApprovalBy(operatorId);
        release.setApprovedAt(LocalDateTime.now());
        release.setUpdatedBy(operatorId);
        return promptReleaseRepository.save(release);
    }

    public PromptRelease reject(Long releaseId, ReviewReleaseRequest request, Long operatorId) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        PromptRelease release = getRelease(releaseId);
        if (release.getStatus() != ReleaseStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("仅待审核状态可驳回");
        }
        release.setStatus(ReleaseStatus.CANCELLED);
        release.setRollbackReason(request == null ? null : request.getRejectionReason());
        release.setUpdatedBy(operatorId);
        return promptReleaseRepository.save(release);
    }

    public PromptRelease execute(Long releaseId, Long operatorId) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        PromptRelease release = getRelease(releaseId);
        if (release.getStatus() != ReleaseStatus.APPROVED) {
            throw new IllegalArgumentException("仅审核通过状态可执行发布");
        }
        release.setStatus(ReleaseStatus.RELEASING);
        release.setExecutedBy(operatorId);
        release.setExecutedAt(LocalDateTime.now());
        promptReleaseRepository.save(release);

        PromptLiveVersion live = promptLiveVersionRepository
            .findByPsuIdAndEnvironment(release.getPsuId(), release.getEnvironment())
            .orElseGet(() -> {
                PromptLiveVersion item = new PromptLiveVersion();
                item.setPsuId(release.getPsuId());
                item.setEnvironment(release.getEnvironment());
                item.setStableReleaseId(release.getId());
                item.setStableRevisionNo(release.getTargetRevisionNo());
                return item;
            });

        if (release.getReleaseType() == ReleaseType.FULL) {
            live.setStableReleaseId(release.getId());
            live.setStableRevisionNo(release.getTargetRevisionNo());
            live.setCanaryReleaseId(null);
        } else {
            // 灰度发布场景：如果没有稳定版本，兜底先指向当前版本。
            if (live.getStableReleaseId() == null) {
                live.setStableReleaseId(release.getId());
                live.setStableRevisionNo(release.getTargetRevisionNo());
            }
            live.setCanaryReleaseId(release.getId());
        }
        live.setUpdatedBy(operatorId);
        promptLiveVersionRepository.save(live);

        release.setStatus(ReleaseStatus.SUCCESS);
        release.setUpdatedBy(operatorId);
        return promptReleaseRepository.save(release);
    }

    public PromptRelease rollback(Long releaseId, RollbackReleaseRequest request, Long operatorId) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        PromptRelease release = getRelease(releaseId);
        // 仅成功发布后的单据允许回滚，避免草稿/审核态误操作。
        if (release.getStatus() != ReleaseStatus.SUCCESS) {
            throw new IllegalArgumentException("仅SUCCESS状态发布单允许回滚");
        }
        Integer targetRevisionNo = request == null ? null : request.getTargetRevisionNo();
        String rollbackReason = request == null ? null : request.getReason();
        if (targetRevisionNo == null) {
            throw new IllegalArgumentException("回滚目标版本号不能为空");
        }

        PromptLiveVersion live = promptLiveVersionRepository
            .findByPsuIdAndEnvironment(release.getPsuId(), release.getEnvironment())
            .orElseThrow(() -> new IllegalArgumentException("环境生效版本不存在，无法回滚"));

        Integer fromRevision = live.getStableRevisionNo();
        Optional<PromptRelease> toReleaseOpt = promptReleaseRepository
            .findTopByPsuIdAndEnvironmentAndTargetRevisionNoAndStatusOrderByUpdatedAtDesc(
                release.getPsuId(),
                release.getEnvironment(),
                targetRevisionNo,
                ReleaseStatus.SUCCESS
            );
        Long toReleaseId = toReleaseOpt.map(PromptRelease::getId).orElse(release.getId());

        live.setStableReleaseId(toReleaseId);
        live.setStableRevisionNo(targetRevisionNo);
        if (Objects.equals(live.getCanaryReleaseId(), releaseId)) {
            live.setCanaryReleaseId(null);
        }
        live.setUpdatedBy(operatorId);
        promptLiveVersionRepository.save(live);

        release.setRollbackToRevisionNo(targetRevisionNo);
        release.setRollbackReason(rollbackReason);
        release.setStatus(ReleaseStatus.ROLLED_BACK);
        release.setUpdatedBy(operatorId);
        promptReleaseRepository.save(release);

        PromptRollbackRecord record = new PromptRollbackRecord();
        record.setPsuId(release.getPsuId());
        record.setEnvironment(release.getEnvironment());
        record.setFromReleaseId(releaseId);
        record.setFromRevisionNo(fromRevision == null ? targetRevisionNo : fromRevision);
        record.setToReleaseId(toReleaseId);
        record.setToRevisionNo(targetRevisionNo);
        record.setReason(rollbackReason);
        record.setOperatorId(operatorId);
        promptRollbackRecordRepository.save(record);
        return release;
    }

    public List<PromptReleaseRule> getRules(Long releaseId) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        getRelease(releaseId);
        return promptReleaseRuleRepository.findByReleaseIdOrderByPriorityAsc(releaseId);
    }

    public PromptReleaseRule addRule(Long releaseId, ReleaseRuleRequest request) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        RequestValidationUtils.requireNonNull(request, "request");
        getRelease(releaseId);
        validateRuleRequest(request);
        PromptReleaseRule rule = new PromptReleaseRule();
        rule.setReleaseId(releaseId);
        fillRule(rule, request);
        return promptReleaseRuleRepository.save(rule);
    }

    public PromptReleaseRule updateRule(Long releaseId, Long ruleId, ReleaseRuleRequest request) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        RequestValidationUtils.requireNonNull(ruleId, "ruleId");
        RequestValidationUtils.requireNonNull(request, "request");
        getRelease(releaseId);
        validateRuleRequest(request);
        Long safeRuleId = Objects.requireNonNull(ruleId);
        PromptReleaseRule rule = promptReleaseRuleRepository.findById(safeRuleId)
            .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + safeRuleId));
        if (!rule.getReleaseId().equals(releaseId)) {
            throw new IllegalArgumentException("规则不属于当前发布单");
        }
        fillRule(rule, request);
        return promptReleaseRuleRepository.save(rule);
    }

    public void deleteRule(Long releaseId, Long ruleId) {
        RequestValidationUtils.requireNonNull(releaseId, "releaseId");
        RequestValidationUtils.requireNonNull(ruleId, "ruleId");
        getRelease(releaseId);
        Long safeRuleId = Objects.requireNonNull(ruleId);
        PromptReleaseRule rule = promptReleaseRuleRepository.findById(safeRuleId)
            .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + safeRuleId));
        if (!rule.getReleaseId().equals(releaseId)) {
            throw new IllegalArgumentException("规则不属于当前发布单");
        }
        promptReleaseRuleRepository.delete(rule);
    }

    public ResolvePromptResponse resolve(ResolvePromptRequest request) {
        if (request == null || request.getPsuId() == null || request.getEnvironment() == null) {
            throw new IllegalArgumentException("psuId/environment不能为空");
        }
        Long safePsuId = Objects.requireNonNull(request.getPsuId());
        String safeEnvironment = request.getEnvironment().trim().toUpperCase();
        psuReleaseVersionRepository.findTopByPsuIdAndTagOrderByUpdatedAtDesc(safePsuId, PsuTag.FORMAL)
            .orElseThrow(() -> new IllegalArgumentException("未找到正式版本，暂不可对外提供服务"));
        PromptLiveVersion live = promptLiveVersionRepository
            .findByPsuIdAndEnvironment(safePsuId, safeEnvironment)
            .orElseThrow(() -> new IllegalArgumentException("未找到生效版本"));

        ResolvePromptResponse response = new ResolvePromptResponse();
        response.setPsuId(safePsuId);
        response.setEnvironment(safeEnvironment);
        response.getRenderConfig().put("placeholderStyle", "mustache");

        Long canaryReleaseId = live.getCanaryReleaseId();
        if (canaryReleaseId != null) {
            PromptRelease canary = promptReleaseRepository.findById(canaryReleaseId).orElse(null);
            if (canary != null) {
                List<PromptReleaseRule> rules = promptReleaseRuleRepository.findByReleaseIdAndEnabledOrderByPriorityAsc(canaryReleaseId, true);
                PromptReleaseRule matched = matchRule(rules, request.getContext());
                if (matched != null) {
                    response.setReleaseId(canaryReleaseId);
                    response.setRevisionNo(canary.getTargetRevisionNo());
                    response.setRouteType("CANARY");
                    response.setRuleId(matched.getId());
                    return response;
                }
            }
        }

        response.setReleaseId(live.getStableReleaseId());
        response.setRevisionNo(live.getStableRevisionNo());
        response.setRouteType("STABLE");
        return response;
    }

    public PromptSchemaResolveResponse getPromptAndSchema(String psuId, PsuTag tag) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonNull(tag, "tag");
        String safePsuId = Objects.requireNonNull(psuId).trim();
        if (safePsuId.isBlank()) {
            throw new IllegalArgumentException("psuId不能为空");
        }
        PsuTag safeTag = Objects.requireNonNull(tag);
        PsuUnit psu = psuRepository.findByPsuId(safePsuId)
            .orElseThrow(() -> new IllegalArgumentException("PSU不存在: " + safePsuId));
        Long psuDbId = psu.getId();

        PsuReleaseVersion releaseVersion = psuReleaseVersionRepository
            .findTopByPsuIdAndTagOrderByUpdatedAtDesc(psuDbId, safeTag)
            .orElseThrow(() -> new IllegalArgumentException("未找到对应标签版本: psuId=" + safePsuId + ", tag=" + safeTag));

        PromptCompositionRevision promptRevision = promptCompositionRevisionRepository
            .findByCompositionIdAndRevisionNo(releaseVersion.getPromptId(), releaseVersion.getPromptVersionNo())
            .orElseThrow(() -> new IllegalArgumentException("未找到Prompt版本快照"));

        JsonSchema schema = jsonSchemaRepository.findById(releaseVersion.getJsonSchemaId())
            .orElseThrow(() -> new IllegalArgumentException("未找到Schema版本"));

        PromptSchemaResolveResponse response = new PromptSchemaResolveResponse();
        response.setPsuId(safePsuId);
        response.setTag(safeTag.name());
        response.setPromptId(releaseVersion.getPromptId());
        response.setPromptVersionNo(releaseVersion.getPromptVersionNo());
        response.setPrompt(promptRevision.getContentSnapshot());
        response.setJsonSchemaId(releaseVersion.getJsonSchemaId());
        response.setJsonSchemaVersionNo(releaseVersion.getJsonSchemaVersionNo());
        response.setJsonSchema(schema.getSchemaContent());
        return response;
    }

    private void validateCreateRequest(CreateReleaseRequest request) {
        if (request == null || request.getPsuId() == null || request.getTargetCompositionId() == null || request.getTargetRevisionNo() == null) {
            throw new IllegalArgumentException("发布单参数不完整");
        }
        if (request.getEnvironment() == null || request.getEnvironment().isBlank()) {
            throw new IllegalArgumentException("environment不能为空");
        }
        if (request.getReleaseType() == null) {
            throw new IllegalArgumentException("releaseType不能为空");
        }
    }

    private void validateRuleRequest(ReleaseRuleRequest request) {
        if (request == null || request.getRuleType() == null) {
            throw new IllegalArgumentException("ruleType不能为空");
        }
        if (request.getRuleType() == ReleaseRuleType.PERCENT) {
            if (request.getTrafficPercent() == null || request.getTrafficPercent() < 0 || request.getTrafficPercent() > 100) {
                throw new IllegalArgumentException("PERCENT规则的trafficPercent必须在0-100");
            }
        }
    }

    private void fillRule(PromptReleaseRule rule, ReleaseRuleRequest request) {
        rule.setRuleType(request.getRuleType());
        rule.setRuleKey(request.getRuleKey());
        rule.setOperator(request.getOperator());
        rule.setRuleValue(request.getRuleValue());
        rule.setTrafficPercent(request.getTrafficPercent());
        rule.setPriority(request.getPriority() == null ? 100 : request.getPriority());
        rule.setEnabled(request.getEnabled() == null ? true : request.getEnabled());
    }

    private PromptReleaseRule matchRule(List<PromptReleaseRule> rules, Map<String, Object> context) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        Map<String, Object> safeContext = context == null ? Map.of() : context;
        for (PromptReleaseRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled())) {
                continue;
            }
            if (rule.getRuleType() == ReleaseRuleType.PERCENT) {
                if (matchPercent(rule, safeContext)) {
                    return rule;
                }
                continue;
            }
            if (matchKeyRule(rule, safeContext)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchPercent(PromptReleaseRule rule, Map<String, Object> context) {
        int percent = rule.getTrafficPercent() == null ? 0 : rule.getTrafficPercent();
        if (percent <= 0) {
            return false;
        }
        String bucketKey = String.valueOf(context.getOrDefault("tenantId", "")) + "|" +
            String.valueOf(context.getOrDefault("userId", "")) + "|" +
            String.valueOf(context.getOrDefault("traceId", ""));
        int hash = Math.abs(bucketKey.hashCode()) % 100;
        return hash < percent;
    }

    private boolean matchKeyRule(PromptReleaseRule rule, Map<String, Object> context) {
        String key = rule.getRuleKey();
        if (key == null || key.isBlank()) {
            return false;
        }
        Object value = context.get(key);
        if (value == null) {
            return false;
        }
        RuleOperator operator = rule.getOperator() == null ? RuleOperator.EQ : rule.getOperator();
        String text = String.valueOf(value);
        String expect = rule.getRuleValue() == null ? "" : rule.getRuleValue();
        if (operator == RuleOperator.EQ) {
            return text.equals(expect);
        }
        if (operator == RuleOperator.IN) {
            String[] arr = expect.split(",");
            for (String item : arr) {
                if (text.equals(item.trim())) {
                    return true;
                }
            }
            return false;
        }
        if (operator == RuleOperator.REGEX) {
            return text.matches(expect);
        }
        return false;
    }
}
