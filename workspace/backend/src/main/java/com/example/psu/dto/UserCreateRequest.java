package com.example.psu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户创建请求DTO
 */
@Data
public class UserCreateRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @NotBlank(message = "角色不能为空")
    private String role;  // 使用字符串而不是枚举
    
    private Boolean enabled = true;
}