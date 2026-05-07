package com.example.psu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Schema更新请求类
 */
@Data
public class UpdateSchemaRequest {

    private Integer baseVersionNo;
    
    @NotBlank(message = "Schema内容不能为空")
    private String schemaContent;
    
    @Size(max = 500, message = "变更日志长度不能超过500个字符")
    private String changeLog;
}
