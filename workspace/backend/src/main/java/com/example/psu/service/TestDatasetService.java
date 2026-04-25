package com.example.psu.service;

import com.example.psu.dto.request.TestDatasetCreateRequest;
import com.example.psu.entity.TestDataset;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.TestDatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 检查PSU是否存在
        psuRepository.findById(safePsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        
        return testDatasetRepository.findByPsuIdOrderByCreatedAtDesc(safePsuId);
    }
    
    /**
     * 创建测试数据集
     * @param psuId PSU ID
     * @param request 创建请求
     * @return 创建的测试数据集
     */
    public TestDataset createTestDataset(Long psuId, TestDatasetCreateRequest request) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        request = RequestValidationUtils.requireNonNull(request, "request");
        RequestValidationUtils.requireNonBlank(request.getName(), "name");
        RequestValidationUtils.requireNonBlank(request.getDataContent(), "dataContent");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 检查PSU是否存在
        psuRepository.findById(safePsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        
        TestDataset dataset = new TestDataset();
        dataset.setPsuId(safePsuId);
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
        RequestValidationUtils.requireNonNull(id, "id");
        request = RequestValidationUtils.requireNonNull(request, "request");
        RequestValidationUtils.requireNonBlank(request.getName(), "name");
        RequestValidationUtils.requireNonBlank(request.getDataContent(), "dataContent");
        Long safeId = Objects.requireNonNull(id);
        TestDataset dataset = testDatasetRepository.findById(safeId)
                .orElseThrow(() -> new RuntimeException("Test dataset not found: " + safeId));
        
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
        RequestValidationUtils.requireNonNull(id, "id");
        Long safeId = Objects.requireNonNull(id);
        if (!testDatasetRepository.existsById(safeId)) {
            throw new RuntimeException("Test dataset not found: " + safeId);
        }
        testDatasetRepository.deleteById(safeId);
    }
}


