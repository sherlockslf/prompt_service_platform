package com.example.psu.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * JSON Schema实体类
 */
@Entity
@Table(name = "ai_prompt_json_schemas", 
       indexes = {
           @Index(name = "idx_json_schemas_psu_id", columnList = "psuId"),
           @Index(name = "idx_json_schemas_version", columnList = "version"),
           @Index(name = "idx_json_schemas_modified_by", columnList = "modifiedBy"),
           @Index(name = "idx_json_schemas_created", columnList = "createdAt")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"psuId", "version"})
       })
@Data
public class JsonSchema {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long psuId;                // 关联PSU ID
    
    @Column(nullable = false, columnDefinition = "JSON")
    private String schemaContent;      // JSON Schema内容
    
    @Column(nullable = false)
    private Integer version;           // 版本号
    
    @Column(nullable = false)
    private Long modifiedBy;           // 修改者ID（仅研发）
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;   // 创建时间
    
    private String changeLog;          // 变更日志
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}