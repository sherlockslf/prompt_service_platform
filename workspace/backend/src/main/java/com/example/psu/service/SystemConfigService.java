package com.example.psu.service;

import com.example.psu.entity.SystemConfig;
import com.example.psu.enums.ConfigType;
import com.example.psu.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置服务
 */
@Service
public class SystemConfigService {
    
    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Value("${llm.api-key:}")
    private String llmApiKey;
    
    /**
     * 获取所有系统配置
     * @return 配置列表
     */
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }
    
    /**
     * 根据配置键获取配置
     * @param configKey 配置键
     * @return 系统配置
     */
    public SystemConfig getConfigByKey(String configKey) {
        return systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new RuntimeException("Config not found: " + configKey));
    }
    
    /**
     * 创建或更新系统配置
     * @param configKey 配置键
     * @param configValue 配置值
     * @param configType 配置类型
     * @return 系统配置
     */
    public SystemConfig saveConfig(String configKey, String configValue, ConfigType configType) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey).orElse(new SystemConfig());
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        config.setConfigType(configType);
        return systemConfigRepository.save(config);
    }
    
    /**
     * 删除系统配置
     * @param id 配置ID
     */
    public void deleteConfig(Long id) {
        if (!systemConfigRepository.existsById(id)) {
            throw new RuntimeException("Config not found: " + id);
        }
        systemConfigRepository.deleteById(id);
    }

    /**
     * 获取一个可用的DashScope API Key
     * 优先读取配置文件llm.api-key（逗号分隔时取第一个），失败时回退系统配置表。
     * @return 任意一个可用API Key
     */
    public String getAnyDashscopeApiKey() {
        if (llmApiKey != null && !llmApiKey.isBlank()) {
            String[] keys = llmApiKey.split(",");
            for (String key : keys) {
                if (key != null && !key.trim().isEmpty()) {
                    return key.trim();
                }
            }
        }

        return systemConfigRepository.findByConfigKey("default_api_key")
            .map(SystemConfig::getConfigValue)
            .filter(value -> value != null && !value.isBlank())
            .orElseThrow(() -> new RuntimeException("DashScope API Key not configured"));
    }
}
