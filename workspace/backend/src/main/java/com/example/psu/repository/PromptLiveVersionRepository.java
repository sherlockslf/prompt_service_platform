package com.example.psu.repository;

import com.example.psu.entity.PromptLiveVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 生效版本指针仓储
 */
public interface PromptLiveVersionRepository extends JpaRepository<PromptLiveVersion, Long> {
    Optional<PromptLiveVersion> findByPsuIdAndEnvironment(Long psuId, String environment);
}
