package com.example.psu.dto.response;

import lombok.Data;

/**
 * 版本对比响应
 */
@Data
public class VersionCompareResponse {
    private Long psuId;
    private Integer fromVersionNo;
    private Integer toVersionNo;
    private boolean changed;
    private int fromLineCount;
    private int toLineCount;
    private int addedLineCount;
    private int removedLineCount;
    private String fromPrompt;
    private String toPrompt;
}
