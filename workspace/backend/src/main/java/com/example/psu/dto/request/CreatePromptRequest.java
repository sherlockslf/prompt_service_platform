package com.example.psu.dto.request;

import com.example.psu.enums.FragmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建Prompt片段请求类
 */
@Data
public class CreatePromptRequest {
    
    @NotNull(message = "PSU ID不能为空")
    private Long psuId;
    
    @NotBlank(message = "片段标识不能为空")
    @Size(max = 100, message = "片段标识长度不能超过100个字符")
    private String fragmentKey;
    
    @NotBlank(message = "Prompt内容不能为空")
    @Size(max = 10000, message = "Prompt内容长度不能超过10000个字符")
    private String content;
    
    private Boolean editable = true;
    
    @NotNull(message = "片段类型不能为空")
    private FragmentType type = FragmentType.MESSAGE_TEMPLATE;
    
    private Integer sortOrder = 0;
}