package com.example.psu.repository;

import com.example.psu.entity.EvaluationItemResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评估明细仓储
 */
@Repository
public interface EvaluationItemResultRepository extends JpaRepository<EvaluationItemResult, Long> {
    List<EvaluationItemResult> findByTaskIdOrderByIdAsc(Long taskId);

    void deleteByTaskId(Long taskId);
}

