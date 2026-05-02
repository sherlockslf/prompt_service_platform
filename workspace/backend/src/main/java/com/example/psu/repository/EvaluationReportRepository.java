package com.example.psu.repository;

import com.example.psu.entity.EvaluationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 评估报告仓储
 */
@Repository
public interface EvaluationReportRepository extends JpaRepository<EvaluationReport, Long> {
    Optional<EvaluationReport> findByTaskId(Long taskId);
}

