package com.example.psu.enums;

/**
 * PSU状态枚举
 */
public enum PsuStatus {
    DRAFT("DRAFT", "草稿"),
    CANDIDATE("CANDIDATE", "发布候选"),
    FORMAL("FORMAL", "正式版本"),
    ARCHIVED("ARCHIVED", "已归档");
    
    private final String code;
    private final String description;
    
    PsuStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static PsuStatus fromCode(String code) {
        for (PsuStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
