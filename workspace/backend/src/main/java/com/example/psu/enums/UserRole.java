package com.example.psu.enums;

/**
 * 用户角色枚举
 */
public enum UserRole {
    ADMIN("ADMIN", "超级管理员"),
    DEVELOPER("DEVELOPER", "研发人员"),
    BUSINESS("BUSINESS", "产品/运营人员");
    
    private final String code;
    private final String description;
    
    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role code: " + code);
    }
}
