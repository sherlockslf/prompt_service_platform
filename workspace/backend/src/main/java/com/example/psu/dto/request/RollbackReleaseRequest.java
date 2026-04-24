package com.example.psu.dto.request;

import lombok.Data;

/**
 * 发布回滚请求
 */
@Data
public class RollbackReleaseRequest {
    private Integer targetRevisionNo;
    private String reason;
}
