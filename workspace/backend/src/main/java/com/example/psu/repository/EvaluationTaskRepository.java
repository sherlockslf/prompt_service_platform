package com.example.psu.repository;

import com.example.psu.entity.EvaluationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评估任务仓储
 */
@Repository
public interface EvaluationTaskRepository extends JpaRepository<EvaluationTask, Long> {
    List<EvaluationTask> findTop20ByPsuIdOrderByCreatedAtDesc(Long psuId);

    List<EvaluationTask> findTop50ByPsuIdOrderByCreatedAtDesc(Long psuId);

    List<EvaluationTask> findTop50ByPsuIdAndDatasetIdOrderByCreatedAtDesc(Long psuId, Long datasetId);
}
