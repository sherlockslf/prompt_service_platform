package com.example.psu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 参数集更新请求
 */
@Data
public class UpdateParamSetRequest {

    @NotBlank(message = "参数集内容不能为空")
    private String paramSetContent;

    @Size(max = 500, message = "变更日志长度不能超过500个字符")
    private String changeLog;
}
