package com.example.psu.entity;

import com.example.psu.enums.ConfigType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 */
@Entity
@Table(name = "ai_prompt_system_configs", 
       indexes = {
           @Index(name = "idx_system_configs_key", columnList = "configKey"),
           @Index(name = "idx_system_configs_type", columnList = "configType")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "configKey")
       })
@Data
public class SystemConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String configKey;          // 配置键
    
    @Column(nullable = false)
    private String configValue;        // 配置值（加密存储）
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigType configType;     // 配置类型：API_KEY/OTHER
    
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