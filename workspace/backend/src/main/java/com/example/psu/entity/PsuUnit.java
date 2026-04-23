package com.example.psu.entity;

import com.example.psu.enums.PsuStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * PSU单元实体类
 */
@Entity
@Table(name = "ai_prompt_psu", 
       indexes = {
           @Index(name = "idx_psu_units_psu_id", columnList = "psuId"),
           @Index(name = "idx_psu_units_status", columnList = "status"),
           @Index(name = "idx_psu_units_creator", columnList = "creatorId"),
           @Index(name = "idx_psu_units_created", columnList = "createdAt")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "psuId")
       })
@Data
public class PsuUnit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String psuId;              // 全局唯一PSU ID
    
    @Column(nullable = false)
    private String name;               // PSU名称
    
    private String description;        // 描述
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PsuStatus status;          // 状态：ACTIVE/ARCHIVED
    
    @Column(nullable = false)
    private Long creatorId;            // 创建者ID（研发）
    
    @Column(nullable = false)
    private Integer majorVersion = 0;  // 主版本号
    
    @Column(nullable = false)
    private Integer minorVersion = 0;  // 次版本号
    
    @Column(nullable = false)
    private Integer patchVersion = 0;  // 修订版本号
    
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