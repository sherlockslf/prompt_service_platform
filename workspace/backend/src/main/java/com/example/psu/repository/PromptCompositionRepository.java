package com.example.psu.repository;

import com.example.psu.entity.PromptComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 编排草稿仓储
 */
@Repository
public interface PromptCompositionRepository extends JpaRepository<PromptComposition, Long> {

    Optional<PromptComposition> findByPsuId(Long psuId);
}
