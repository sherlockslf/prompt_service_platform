package com.example.psu.repository;

import com.example.psu.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测试运行主表仓储
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供测试运行主记录查询能力
 */
@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {
    List<TestRun> findTop50ByPsuIdOrderByCreatedAtDesc(Long psuId);

    List<TestRun> findTop50ByPsuIdAndDatasetIdOrderByCreatedAtDesc(Long psuId, Long datasetId);
}
