package com.example.psu.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 测试数据集实体类
 */
@Entity
@Table(name = "ai_prompt_test_datasets", 
       indexes = {
           @Index(name = "idx_test_datasets_psu_id", columnList = "psuId")
       })
@Data
public class TestDataset {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long psuId;                // 关联PSU ID
    
    @Column(nullable = false)
    private String name;               // 数据集名称
    
    @Column(nullable = false, length = 10000)
    private String dataContent;        // 测试数据内容（JSON格式）
    
    @Column(length = 500)
    private String description;        // 描述
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;   // 创建时间
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;   // 更新时间
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
