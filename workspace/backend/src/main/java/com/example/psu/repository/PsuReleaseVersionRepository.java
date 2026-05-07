package com.example.psu.repository;

import com.example.psu.entity.PsuReleaseVersion;
import com.example.psu.enums.PsuTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PsuReleaseVersionRepository extends JpaRepository<PsuReleaseVersion, Long> {
    Optional<PsuReleaseVersion> findByPsuIdAndPsuVersionNo(Long psuId, Integer psuVersionNo);
    List<PsuReleaseVersion> findByPsuIdAndTag(Long psuId, PsuTag tag);
    Optional<PsuReleaseVersion> findTopByPsuIdAndTagOrderByUpdatedAtDesc(Long psuId, PsuTag tag);
    Optional<PsuReleaseVersion> findByPsuIdAndPromptIdAndPromptVersionNoAndTag(
        Long psuId,
        Long promptId,
        Integer promptVersionNo,
        PsuTag tag
    );
}
