package com.example.psu.entity;

import com.example.psu.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Entity
@Table(name = "ai_prompt_users", 
       indexes = {
           @Index(name = "idx_users_username", columnList = "username"),
           @Index(name = "idx_users_role", columnList = "role"),
           @Index(name = "idx_users_enabled", columnList = "enabled")
       })
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;           // 用户名
    
    @Column(nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String password;           // 明文密码
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;             // 角色：ADMIN/DEVELOPER/BUSINESS
    
    @Column(nullable = false)
    private Boolean enabled;           // 启用状态
    
    @Column(length = 20)
    private String phoneNumber;        // 手机号码
    
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