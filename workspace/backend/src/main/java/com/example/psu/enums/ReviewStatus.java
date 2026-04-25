package com.example.psu.enums;

/**
 * 版本审核状态枚举
 */
public enum ReviewStatus {
    DRAFT("DRAFT", "草稿"),
    CANDIDATE("CANDIDATE", "发布候选"),
    FORMAL("FORMAL", "正式版本"),
    ARCHIVED("ARCHIVED", "已归档");
    
    private final String code;
    private final String description;
    
    ReviewStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ReviewStatus fromCode(String code) {
        for (ReviewStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown review status code: " + code);
    }
}
