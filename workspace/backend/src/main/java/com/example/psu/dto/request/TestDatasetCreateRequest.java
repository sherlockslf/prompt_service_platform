package com.example.psu.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 测试数据集创建请求
 */
@Data
public class TestDatasetCreateRequest {
    
    @NotBlank(message = "数据集名称不能为空")
    private String name;
    
    @NotBlank(message = "测试数据内容不能为空")
    private String dataContent;
    
    private String description;
}
