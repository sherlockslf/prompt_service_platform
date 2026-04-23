package com.example.psu.repository;

import com.example.psu.entity.PromptFragment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Prompt片段仓库接口
 */
@Repository
public interface PromptFragmentRepository extends JpaRepository<PromptFragment, Long> {
    
    /**
     * 根据PSU ID查找所有Prompt片段
     * @param psuId PSU ID
     * @return Prompt片段列表
     */
    List<PromptFragment> findByPsuIdOrderBySortOrderAsc(Long psuId);
    
    /**
     * 根据PSU ID和片段键查找
     * @param psuId PSU ID
     * @param fragmentKey 片段键
     * @return Prompt片段
     */
    Optional<PromptFragment> findByPsuIdAndFragmentKey(Long psuId, String fragmentKey);
}