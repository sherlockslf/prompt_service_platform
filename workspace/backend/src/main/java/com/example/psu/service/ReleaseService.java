package com.example.psu.service;

import com.example.psu.dto.request.CreateReleaseRequest;
import com.example.psu.dto.request.ReleaseRuleRequest;
import com.example.psu.dto.request.ResolvePromptRequest;
import com.example.psu.dto.request.ReviewReleaseRequest;
import com.example.psu.dto.request.RollbackReleaseRequest;
import com.example.psu.dto.response.ResolvePromptResponse;
import com.example.psu.entity.PromptLiveVersion;
import com.example.psu.entity.PromptRelease;
import com.example.psu.entity.PromptReleaseRule;
import com.example.psu.entity.PromptRollbackRecord;
import com.example.psu.enums.ReleaseRuleType;
import com.example.psu.enums.ReleaseStatus;
import com.example.psu.enums.ReleaseType;
import com.example.psu.enums.RuleOperator;
import com.example.psu.repository.PromptCompositionRevisionRepository;
import com.example.psu.repository.PromptLiveVersionRepository;
import com.example.psu.repository.PromptReleaseRepository;
import com.example.psu.repository.PromptReleaseRuleRepository;
import com.example.psu.repository.PromptRollbackRecordRepository;
import com.example.psu.repository.PsuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final PsuRepository psuRepository;

    public ReleaseService(
        PromptReleaseRepository promptReleaseRepository,
        PromptReleaseRuleRepository promptReleaseRuleRepository,
        PromptLiveVersionRepository promptLiveVersionRepository,
        PromptRollbackRecordRepository promptRollbackRecordRepository,
        PromptCompositionRevisionRepository promptCompositionRevisionRepository,
        PsuRepository psuRepository
    ) {
        this.promptReleaseRepository = promptReleaseRepository;
        this.promptReleaseRuleRepository = promptReleaseRuleRepository;
        this.promptLiveVersionRepository = promptLiveVersionRepository;
        this.promptRollbackRecordRepository = promptRollbackRecordRepository;
        this.promptCompositionRevisionRepository = promptCompositionRevisionRepository;
        this.psuRepository = psuRepository;
    }

    public Page<PromptRelease> getReleases(Long psuId, String environment, Pageable pageable) {
        if (psuId == null) {
            return promptReleaseRepository.findAll(pageable);
        }
        if (environment != null && !environment.isBlank()) {
            return promptReleaseRepository.findByPsuIdAndEnvironment(psuId, environment, pageable);
        }
        return promptReleaseRepository.findByPsuId(psuId, pageable);
    }

    public PromptRelease getRelease(Long releaseId) {
        return promptReleaseRepository.findById(releaseId)
            .orElseThrow(() -> new IllegalArgumentException("发布单不存在: " + releaseId));
    }

    public PromptRelease createRelease(CreateReleaseRequest request, Long operatorId) {
        validateCreateRequest(request);
        psuRepository.findById(request.getPsuId())
            .orElseThrow(() -> new IllegalArgumentException("PSU不存在: " + request.getPsuId()));
        promptCompositionRevisionRepository
            .findByCompositionIdAndRevisionNo(request.getTargetCompositionId(), request.getTargetRevisionNo())
            .orElseThrow(() -> new IllegalArgumentException("目标快照不存在"));

        PromptRelease release = new PromptRelease();
        release.setPsuId(request.getPsuId());
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
        PromptRelease release = getRelease(releaseId);
        if (release.getStatus() != ReleaseStatus.DRAFT) {
            throw new IllegalArgumentException("仅DRAFT状态可提交审核");
        }
        release.setStatus(ReleaseStatus.PENDING_APPROVAL);
        release.setUpdatedBy(operatorId);
        return promptReleaseRepository.save(release);
    }

    public PromptRelease approve(Long releaseId, Long operatorId) {
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
        PromptRelease release = getRelease(releaseId);
        Integer targetRevisionNo = request == null ? null : request.getTargetRevisionNo();
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
        if (live.getCanaryReleaseId() != null && live.getCanaryReleaseId().equals(releaseId)) {
            live.setCanaryReleaseId(null);
        }
        live.setUpdatedBy(operatorId);
        promptLiveVersionRepository.save(live);

        release.setRollbackToRevisionNo(targetRevisionNo);
        release.setRollbackReason(request.getReason());
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
        record.setReason(request.getReason());
        record.setOperatorId(operatorId);
        promptRollbackRecordRepository.save(record);
        return release;
    }

    public List<PromptReleaseRule> getRules(Long releaseId) {
        getRelease(releaseId);
        return promptReleaseRuleRepository.findByReleaseIdOrderByPriorityAsc(releaseId);
    }

    public PromptReleaseRule addRule(Long releaseId, ReleaseRuleRequest request) {
        getRelease(releaseId);
        validateRuleRequest(request);
        PromptReleaseRule rule = new PromptReleaseRule();
        rule.setReleaseId(releaseId);
        fillRule(rule, request);
        return promptReleaseRuleRepository.save(rule);
    }

    public PromptReleaseRule updateRule(Long releaseId, Long ruleId, ReleaseRuleRequest request) {
        getRelease(releaseId);
        validateRuleRequest(request);
        PromptReleaseRule rule = promptReleaseRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + ruleId));
        if (!rule.getReleaseId().equals(releaseId)) {
            throw new IllegalArgumentException("规则不属于当前发布单");
        }
        fillRule(rule, request);
        return promptReleaseRuleRepository.save(rule);
    }

    public void deleteRule(Long releaseId, Long ruleId) {
        getRelease(releaseId);
        PromptReleaseRule rule = promptReleaseRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + ruleId));
        if (!rule.getReleaseId().equals(releaseId)) {
            throw new IllegalArgumentException("规则不属于当前发布单");
        }
        promptReleaseRuleRepository.delete(rule);
    }

    public ResolvePromptResponse resolve(ResolvePromptRequest request) {
        if (request == null || request.getPsuId() == null || request.getEnvironment() == null) {
            throw new IllegalArgumentException("psuId/environment不能为空");
        }
        PromptLiveVersion live = promptLiveVersionRepository
            .findByPsuIdAndEnvironment(request.getPsuId(), request.getEnvironment().trim().toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("未找到生效版本"));

        ResolvePromptResponse response = new ResolvePromptResponse();
        response.setPsuId(request.getPsuId());
        response.setEnvironment(request.getEnvironment().trim().toUpperCase());
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
