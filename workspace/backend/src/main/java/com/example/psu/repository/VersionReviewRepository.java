package com.example.psu.repository;

import com.example.psu.entity.VersionReview;
import com.example.psu.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 版本审核仓储
 */
@Repository
public interface VersionReviewRepository extends JpaRepository<VersionReview, Long> {

    List<VersionReview> findByPsuIdOrderBySubmittedAtDesc(Long psuId);

    List<VersionReview> findAllByOrderBySubmittedAtDesc();

    Page<VersionReview> findByPsuId(Long psuId, Pageable pageable);

    Page<VersionReview> findAll(Pageable pageable);

    Optional<VersionReview> findTopByPsuIdAndStatusOrderByReviewedAtDesc(Long psuId, ReviewStatus status);

    boolean existsByCompositionIdAndCompositionRevisionNoAndStatus(
        Long compositionId,
        Integer compositionRevisionNo,
        ReviewStatus status
    );
}
