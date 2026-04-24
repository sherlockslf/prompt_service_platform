package com.example.psu.repository;

import com.example.psu.entity.PromptRelease;
import com.example.psu.enums.ReleaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 发布单仓储
 */
public interface PromptReleaseRepository extends JpaRepository<PromptRelease, Long> {
    Page<PromptRelease> findByPsuId(Long psuId, Pageable pageable);
    Page<PromptRelease> findByPsuIdAndEnvironment(Long psuId, String environment, Pageable pageable);
    Optional<PromptRelease> findTopByPsuIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(Long psuId, String environment, ReleaseStatus status);
    Optional<PromptRelease> findTopByPsuIdAndEnvironmentAndTargetRevisionNoAndStatusOrderByUpdatedAtDesc(
        Long psuId,
        String environment,
        Integer targetRevisionNo,
        ReleaseStatus status
    );
}
