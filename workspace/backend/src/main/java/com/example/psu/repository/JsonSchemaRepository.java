package com.example.psu.repository;

import com.example.psu.entity.JsonSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JSON Schema仓库接口
 */
@Repository
public interface JsonSchemaRepository extends JpaRepository<JsonSchema, Long> {
    Optional<JsonSchema> findByPsuId(Long psuId);
    
    /**
     * 根据PSU ID查找最新的Schema
     * @param psuId PSU ID
     * @return 最新的Schema
     */
    Optional<JsonSchema> findTopByPsuIdOrderByVersionDesc(Long psuId);
    
    /**
     * 根据PSU ID查找所有Schema版本
     * @param psuId PSU ID
     * @return Schema版本列表
     */
    List<JsonSchema> findByPsuIdOrderByVersionDesc(Long psuId);
}
