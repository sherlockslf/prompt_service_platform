package com.example.psu.repository;

import com.example.psu.entity.PromptRollbackRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 回滚记录仓储
 */
public interface PromptRollbackRecordRepository extends JpaRepository<PromptRollbackRecord, Long> {
    List<PromptRollbackRecord> findByPsuIdAndEnvironmentOrderByCreatedAtDesc(Long psuId, String environment);
}
