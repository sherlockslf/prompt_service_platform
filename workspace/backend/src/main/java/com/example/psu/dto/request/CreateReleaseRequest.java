package com.example.psu.dto.request;

import com.example.psu.enums.ReleaseType;
import lombok.Data;

/**
 * 创建发布单请求
 */
@Data
public class CreateReleaseRequest {
    private Long psuId;
    private String environment;
    private ReleaseType releaseType;
    private Long targetCompositionId;
    private Integer targetRevisionNo;
    private Integer baseRevisionNo;
}
