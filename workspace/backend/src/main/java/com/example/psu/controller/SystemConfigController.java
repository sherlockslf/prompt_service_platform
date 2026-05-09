package com.example.psu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.psu.entity.SystemConfig;
import com.example.psu.enums.ConfigType;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.SystemConfigService;

/**
 * 系统配置控制器（仅管理员使用）
 */
@RestController
@RequestMapping("/api/configs")
public class SystemConfigController {
    
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private AsyncDispatchService asyncDispatchService;
    
    /**
     * 查询全部系统配置。
     * 请求方法与路径：GET /api/configs（兼容 /api/v1/configs）。
     * 入参：无。
     * 返回：SystemConfig 列表。
     */
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * 按配置键查询配置项。
     * 请求方法与路径：GET /api/configs/by-configKey（兼容 /api/v1/...）。
     * 入参：configKey。
     * 返回：SystemConfig。
     */
    @GetMapping("/by-configKey")
    public ResponseEntity<SystemConfig> getConfigByKey(@RequestParam String configKey) {
        SystemConfig config = systemConfigService.getConfigByKey(configKey);
        return ResponseEntity.ok(config);
    }

    /**
     * 查询可用 DashScope API Key（脱敏交付建议在网关层处理）。
     * 请求方法与路径：GET /api/configs/dashscope-key（兼容 /api/v1/...）。
     * 入参：无。
     * 返回：{"apiKey": "..."}。
     */
    @GetMapping("/dashscope-key")
    public ResponseEntity<Map<String, String>> getDashscopeKey() {
        return ResponseEntity.ok(Map.of("apiKey", systemConfigService.getAnyDashscopeApiKey()));
    }
    
    /**
     * 创建或更新系统配置（同步）。
     * 请求方法与路径：POST /api/configs（兼容 /api/v1/...）。
     * 入参：requestBody（configKey、configValue、configType）。
     * 返回：持久化后的 SystemConfig。
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
     * 创建或更新系统配置（异步）。
     * 请求方法与路径：POST /api/configs/async（兼容 /api/v1/...）。
     * 入参：requestBody（configKey、configValue、configType）。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/async")
    public ResponseEntity<String> saveConfigAsync(@RequestBody Map<String, String> requestBody) {
        String configKey = requestBody.get("configKey");
        String configValue = requestBody.get("configValue");
        String configType = requestBody.getOrDefault("configType", "OTHER");
        asyncDispatchService.dispatch(() -> systemConfigService.saveConfig(
            configKey,
            configValue,
            ConfigType.fromCode(configType.toUpperCase())
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 删除系统配置（同步）。
     * 请求方法与路径：DELETE /api/configs/by-id（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：200 OK。
     */
    @DeleteMapping("/by-id")
    public ResponseEntity<Void> deleteConfig(@RequestParam Long id) {
        systemConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除系统配置（异步）。
     * 请求方法与路径：DELETE /api/configs/by-id/async（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：202 ACCEPTED。
     */
    @DeleteMapping("/by-id/async")
    public ResponseEntity<String> deleteConfigAsync(@RequestParam Long id) {
        asyncDispatchService.dispatch(() -> systemConfigService.deleteConfig(id));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
}




