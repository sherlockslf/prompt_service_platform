package com.example.psu.enums;

/**
 * Prompt片段类型枚举
 */
public enum FragmentType {
    CORE_RULES("CORE_RULES", "核心规则片段"),
    MESSAGE_TEMPLATE("MESSAGE_TEMPLATE", "消息模板片段");
    
    private final String code;
    private final String description;
    
    FragmentType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static FragmentType fromCode(String code) {
        for (FragmentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown fragment type code: " + code);
    }
}
