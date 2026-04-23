package com.example.psu.service;

import com.example.psu.dto.request.TestDatasetCreateRequest;
import com.example.psu.entity.PsuUnit;
import com.example.psu.entity.TestDataset;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.TestDatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 测试数据集管理服务
 */
@Service
public class TestDatasetService {
    
    @Autowired
    private TestDatasetRepository testDatasetRepository;
    
    @Autowired
    private PsuRepository psuRepository;
    
    /**
     * 获取PSU的测试数据集列表
     * @param psuId PSU ID
     * @return 测试数据集列表
     */
    public List<TestDataset> getTestDatasets(Long psuId) {
        // 检查PSU是否存在
        psuRepository.findById(psuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + psuId));
        
        return testDatasetRepository.findByPsuIdOrderByCreatedAtDesc(psuId);
    }
    
    /**
     * 创建测试数据集
     * @param psuId PSU ID
     * @param request 创建请求
     * @return 创建的测试数据集
     */
    public TestDataset createTestDataset(Long psuId, TestDatasetCreateRequest request) {
        // 检查PSU是否存在
        psuRepository.findById(psuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + psuId));
        
        TestDataset dataset = new TestDataset();
        dataset.setPsuId(psuId);
        dataset.setName(request.getName());
        dataset.setDataContent(request.getDataContent());
        dataset.setDescription(request.getDescription());
        
        return testDatasetRepository.save(dataset);
    }
    
    /**
     * 更新测试数据集
     * @param id 数据集ID
     * @param request 更新请求
     * @return 更新后的数据集
     */
    public TestDataset updateTestDataset(Long id, TestDatasetCreateRequest request) {
        TestDataset dataset = testDatasetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test dataset not found: " + id));
        
        dataset.setName(request.getName());
        dataset.setDataContent(request.getDataContent());
        dataset.setDescription(request.getDescription());
        
        return testDatasetRepository.save(dataset);
    }
    
    /**
     * 删除测试数据集
     * @param id 数据集ID
     */
    public void deleteTestDataset(Long id) {
        if (!testDatasetRepository.existsById(id)) {
            throw new RuntimeException("Test dataset not found: " + id);
        }
        testDatasetRepository.deleteById(id);
    }
}
