package com.example.psu.repository;

import com.example.psu.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 测试运行主表仓储
 */
@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {
}
