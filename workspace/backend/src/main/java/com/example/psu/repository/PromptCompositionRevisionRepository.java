package com.example.psu.repository;

import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.enums.CompositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 编排快照仓储
 */
@Repository
public interface PromptCompositionRevisionRepository extends JpaRepository<PromptCompositionRevision, Long> {

    Optional<PromptCompositionRevision> findTopByCompositionIdOrderByRevisionNoDesc(Long compositionId);

    Optional<PromptCompositionRevision> findTopByPsuIdAndStatusAtTimeOrderByRevisionNoDesc(
        Long psuId,
        CompositionStatus statusAtTime
    );
}
