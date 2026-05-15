package com.example.psu.service;

import com.example.psu.dto.request.ReviewRequest;
import com.example.psu.dto.request.RollbackVersionRequest;
import com.example.psu.dto.response.VersionCompareResponse;
import com.example.psu.dto.response.VersionReviewSnapshotResponse;
import com.example.psu.entity.VersionReview;
import com.example.psu.enums.PsuTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface VersionReviewService {

    Page<VersionReview> getVersionReviews(Long psuId, Pageable pageable);

    Optional<VersionReview> getVersionReviewById(Long id);

    VersionReview submitVersion(Long psuId, Long submitterId);
    VersionReview submitVersion(Long psuId, Integer versionNo, Long submitterId);

    VersionReview reviewVersion(Long reviewId, ReviewRequest request, Long reviewerId);

    String getCode(Long psuId, String language);
    String getCode(Long psuId, Integer versionNo, String language);
    byte[] downloadCodeBundle(Long psuId, Integer versionNo, String language);

    VersionReview registerGitCommit(Long reviewId, String gitCommitHash, Long operatorId);

    VersionCompareResponse compareVersions(Long psuId, Integer fromVersionNo, Integer toVersionNo);

    VersionReview rollbackVersion(Long psuId, RollbackVersionRequest request, Long operatorId);

    VersionReview assignVersionTag(Long reviewId, PsuTag tag, Long operatorId);

    VersionReviewSnapshotResponse getReviewSnapshot(Long reviewId);
}
