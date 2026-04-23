package com.example.psu.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计日志实体类
 */
@Entity
@Table(name = "ai_prompt_audit_logs", 
       indexes = {
           @Index(name = "idx_audit_logs_user_id", columnList = "userId"),
           @Index(name = "idx_audit_logs_operation", columnList = "operation"),
           @Index(name = "idx_audit_logs_target_type", columnList = "targetType"),
           @Index(name = "idx_audit_logs_target_id", columnList = "targetId"),
           @Index(name = "idx_audit_logs_created", columnList = "createdAt")
       })
@Data
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;               // 操作用户ID
    
    @Column(nullable = false)
    private String username;           // 操作用户名
    
    @Column(nullable = false)
    private String operation;          // 操作类型
    
    @Column(nullable = false)
    private String targetType;         // 目标类型
    
    private Long targetId;             // 目标ID
    
    @Column(columnDefinition = "JSON")
    private String details;            // 操作详情（JSON格式）
    
    private String ipAddress;          // IP地址
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;   // 操作时间
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}