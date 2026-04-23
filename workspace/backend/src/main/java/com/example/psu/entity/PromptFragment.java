package com.example.psu.entity;

import com.example.psu.enums.FragmentType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Prompt片段实体类
 */
@Entity
@Table(name = "ai_prompt_prompt_fragments", 
       indexes = {
           @Index(name = "idx_prompt_fragments_psu_id", columnList = "psuId"),
           @Index(name = "idx_prompt_fragments_type", columnList = "type"),
           @Index(name = "idx_prompt_fragments_editable", columnList = "editable"),
           @Index(name = "idx_prompt_fragments_sort", columnList = "sortOrder")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"psuId", "fragmentKey"})
       })
@Data
public class PromptFragment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long psuId;                // 关联PSU ID
    
    @Column(nullable = false)
    private String fragmentKey;        // 片段标识
    
    @Column(nullable = false, length = 10000)
    private String content;            // Prompt内容
    
    @Column(nullable = false)
    private Boolean editable;          // 是否可编辑（false=已定版锁定，true=可编辑）
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FragmentType type;         // 类型：CORE_RULES/MESSAGE_TEMPLATE
    
    @Column(nullable = false)
    private Integer sortOrder;             // 排序
    
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