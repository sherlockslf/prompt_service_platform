package com.example.psu.dto.response;

import lombok.Data;

/**
 * 版本审核快照响应
 */
@Data
public class VersionReviewSnapshotResponse {
    private Long reviewId;
    private Long psuId;
    private Integer versionNo;
    private Integer schemaVersionNo;
    private Long compositionId;
    private Integer compositionRevisionNo;
    private Integer schemaVersionAtTime;
    private String specJsonSnapshot;
    private String promptContentSnapshot;
}
