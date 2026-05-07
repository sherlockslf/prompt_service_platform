package com.example.psu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Prompt更新请求类
 */
@Data
public class UpdatePromptRequest {

    private Integer baseVersionNo;
    
    @NotBlank(message = "Prompt内容不能为空")
    @Size(max = 10000, message = "Prompt内容长度不能超过10000个字符")
    private String content;
}
