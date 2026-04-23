package com.example.psu.repository;

import com.example.psu.entity.TestDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测试数据集仓库接口
 */
@Repository
public interface TestDatasetRepository extends JpaRepository<TestDataset, Long> {
    
    /**
     * 根据PSU ID查找所有测试数据集
     * @param psuId PSU ID
     * @return 测试数据集列表
     */
    List<TestDataset> findByPsuIdOrderByCreatedAtDesc(Long psuId);
}
