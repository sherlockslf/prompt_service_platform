package com.example.psu.repository;

import com.example.psu.entity.ParamSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 参数集仓库
 */
@Repository
public interface ParamSetRepository extends JpaRepository<ParamSet, Long> {
    Optional<ParamSet> findByPsuId(Long psuId);
}
