package com.example.psu.dto.request;

import com.example.psu.enums.RejectionType;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核请求类
 */
@Data
public class ReviewRequest {
    
    private boolean approved;
    
    @Size(max = 500, message = "驳回原因长度不能超过500个字符")
    private String rejectionReason;

    private RejectionType rejectionType;
}
