package com.example.psu.constants;

/**
 * 安全相关常量
 */
public class SecurityConstants {
    
    /**
     * JWT相关常量
     */
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final String JWT_CLAIM_ROLE = "role";
    
    /**
     * 角色常量
     */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DEVELOPER = "DEVELOPER";
    public static final String ROLE_BUSINESS = "BUSINESS";
    
    /**
     * 权限前缀
     */
    public static final String ROLE_PREFIX = "ROLE_";
    
    private SecurityConstants() {
        // 防止实例化
    }
}
