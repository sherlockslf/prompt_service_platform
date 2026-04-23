package com.example.psu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.psu.entity.AuditLog;
import com.example.psu.service.AuditLogService;

/**
 * 审计日志控制器（仅管理员使用）
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    
    @Autowired
    private AuditLogService auditLogService;
    
    /**
     * 管理员审计日志页面获取所有系统审计日志
     * 参数：无
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllAuditLogs();
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 管理员审计日志页面根据用户ID筛选审计日志
     * 参数：userId-用户数据库ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUserId(@PathVariable Long userId) {
        List<AuditLog> logs = auditLogService.getAuditLogsByUserId(userId);
        return ResponseEntity.ok(logs);
    }
}
