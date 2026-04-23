package com.example.psu.repository;

import com.example.psu.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计日志仓库接口
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * 根据用户ID查找审计日志
     * @param userId 用户ID
     * @return 审计日志列表
     */
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据操作类型查找审计日志
     * @param operation 操作类型
     * @return 审计日志列表
     */
    List<AuditLog> findByOperationOrderByCreatedAtDesc(String operation);
    
    /**
     * 获取所有审计日志，按创建时间倒序
     * @return 审计日志列表
     */
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
