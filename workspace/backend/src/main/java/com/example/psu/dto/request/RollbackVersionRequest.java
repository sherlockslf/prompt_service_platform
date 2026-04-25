package com.example.psu.dto.request;

import lombok.Data;

/**
 * 版本回滚请求
 */
@Data
public class RollbackVersionRequest {
    private Integer targetVersionNo;
    private String reason;
}
