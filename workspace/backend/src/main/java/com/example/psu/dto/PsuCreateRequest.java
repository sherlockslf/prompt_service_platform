package com.example.psu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PSU创建请求DTO
 */
@Data
public class PsuCreateRequest {
    
    @NotBlank(message = "PSU ID不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "PSU ID只能包含字母、数字、下划线和连字符")
    @Size(min = 1, max = 100, message = "PSU ID长度必须在1-100之间")
    private String psuId;
    
    @NotBlank(message = "PSU名称不能为空")
    @Size(min = 1, max = 200, message = "PSU名称长度必须在1-200之间")
    private String name;
    
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;
}