package com.example.psu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * 查询全量审计日志。
     * 请求方法与路径：GET /api/audit-logs（兼容 /api/v1/audit-logs）。
     * 入参：无。
     * 返回：审计日志列表（按服务层默认排序规则）。
     * 说明：建议在管理端分页展示，避免一次加载过多日志数据。
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllAuditLogs();
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 按用户筛选审计日志。
     * 请求方法与路径：GET /api/audit-logs/user/by-userId（兼容 /api/v1/...）。
     * 入参：userId（用户数据库主键）。
     * 返回：该用户相关的审计日志列表。
     * 说明：用于追踪单用户的关键操作链路。
     */
    @GetMapping("/user/by-userId")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUserId(@RequestParam Long userId) {
        List<AuditLog> logs = auditLogService.getAuditLogsByUserId(userId);
        return ResponseEntity.ok(logs);
    }
}





