package com.example.psu.repository;

import com.example.psu.entity.PsuVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PsuVersionRepository extends JpaRepository<PsuVersion, Long> {
    List<PsuVersion> findByPsuIdOrderByVersionNoDesc(String psuId);

    Optional<PsuVersion> findByPsuIdAndVersionNo(String psuId, Integer versionNo);
}

