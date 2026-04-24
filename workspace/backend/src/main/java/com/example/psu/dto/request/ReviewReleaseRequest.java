package com.example.psu.dto.request;

import lombok.Data;

/**
 * 发布审核请求
 */
@Data
public class ReviewReleaseRequest {
    private String rejectionReason;
}
