package com.example.psu.repository;

import com.example.psu.entity.TestRunItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测试运行明细仓储
 */
@Repository
public interface TestRunItemRepository extends JpaRepository<TestRunItem, Long> {

    List<TestRunItem> findByRunIdOrderByIdAsc(Long runId);
}
