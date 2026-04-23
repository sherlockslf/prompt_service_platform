package com.example.psu.service;

import com.example.psu.entity.AuditLog;
import com.example.psu.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审计日志服务
 */
@Service
public class AuditLogService {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    /**
     * 获取所有审计日志
     * @return 审计日志列表
     */
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * 根据用户ID获取审计日志
     * @param userId 用户ID
     * @return 审计日志列表
     */
    public List<AuditLog> getAuditLogsByUserId(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 创建审计日志
     * @param auditLog 审计日志实体
     * @return 创建的审计日志
     */
    public AuditLog createAuditLog(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }
}
