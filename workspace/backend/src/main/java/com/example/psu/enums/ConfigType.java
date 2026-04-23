package com.example.psu.enums;

/**
 * 系统配置类型枚举
 */
public enum ConfigType {
    API_KEY("API_KEY", "API密钥"),
    OTHER("OTHER", "其他配置");
    
    private final String code;
    private final String description;
    
    ConfigType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ConfigType fromCode(String code) {
        for (ConfigType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown config type code: " + code);
    }
}
