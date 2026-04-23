package com.example.psu.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    
    // 通用错误码
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // 用户相关错误码
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_DISABLED(1003, "用户已被禁用"),
    INVALID_CREDENTIALS(1004, "用户名或密码错误"),
    
    // PSU相关错误码
    PSU_NOT_FOUND(2001, "PSU不存在"),
    PSU_ALREADY_EXISTS(2002, "PSU ID已存在"),
    PSU_ARCHIVED(2003, "PSU已归档"),
    
    // Schema相关错误码
    SCHEMA_NOT_FOUND(3001, "Schema不存在"),
    SCHEMA_VERSION_CONFLICT(3002, "Schema版本冲突"),
    
    // Prompt相关错误码
    PROMPT_NOT_FOUND(4001, "Prompt片段不存在"),
    PROMPT_LOCKED(4002, "Prompt片段已锁定"),
    
    // 版本审核相关错误码
    VERSION_NOT_FOUND(5001, "版本记录不存在"),
    VERSION_ALREADY_SUBMITTED(5002, "版本已提交"),
    VERSION_ALREADY_REVIEWED(5003, "版本已审核"),
    
    // 测试数据集相关错误码
    DATASET_NOT_FOUND(6001, "测试数据集不存在"),
    
    // 系统配置相关错误码
    CONFIG_NOT_FOUND(7001, "系统配置不存在"),
    CONFIG_KEY_DUPLICATE(7002, "配置键重复");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
