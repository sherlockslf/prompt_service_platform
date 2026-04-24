package com.example.psu.repository;

import com.example.psu.entity.PromptReleaseRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 发布规则仓储
 */
public interface PromptReleaseRuleRepository extends JpaRepository<PromptReleaseRule, Long> {
    List<PromptReleaseRule> findByReleaseIdAndEnabledOrderByPriorityAsc(Long releaseId, Boolean enabled);
    List<PromptReleaseRule> findByReleaseIdOrderByPriorityAsc(Long releaseId);
}
