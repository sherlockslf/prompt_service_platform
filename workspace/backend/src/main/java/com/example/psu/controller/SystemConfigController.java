package com.example.psu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.psu.entity.SystemConfig;
import com.example.psu.enums.ConfigType;
import com.example.psu.service.SystemConfigService;

/**
 * 系统配置控制器（仅管理员使用）
 */
@RestController
@RequestMapping("/api/configs")
public class SystemConfigController {
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    /**
     * 管理员系统配置页面获取所有系统配置列表
     * 参数：无
     */
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * 管理员系统配置页面根据配置键获取特定配置
     * 参数：configKey-配置键名
     */
    @GetMapping("/{configKey}")
    public ResponseEntity<SystemConfig> getConfigByKey(@PathVariable String configKey) {
        SystemConfig config = systemConfigService.getConfigByKey(configKey);
        return ResponseEntity.ok(config);
    }

    /**
     * 开发侧/业务侧Prompt测试页面获取DashScope API Key用于在线模型测试
     * 参数：无
     */
    @GetMapping("/dashscope-key")
    public ResponseEntity<Map<String, String>> getDashscopeKey() {
        return ResponseEntity.ok(Map.of("apiKey", systemConfigService.getAnyDashscopeApiKey()));
    }
    
    /**
     * 管理员系统配置页面创建或更新系统配置
     * 参数：configKey-配置键，configValue-配置值，configType-配置类型
     */
    @PostMapping
    public ResponseEntity<SystemConfig> saveConfig(@RequestBody Map<String, String> requestBody) {
        String configKey = requestBody.get("configKey");
        String configValue = requestBody.get("configValue");
        String configType = requestBody.getOrDefault("configType", "OTHER");
        
        SystemConfig config = systemConfigService.saveConfig(
                configKey, 
                configValue, 
                ConfigType.fromCode(configType.toUpperCase())
        );
        return ResponseEntity.ok(config);
    }
    
    /**
     * 管理员系统配置页面删除系统配置
     * 参数：id-配置数据库ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        systemConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }
}

