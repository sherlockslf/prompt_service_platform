package com.example.psu.enums;

/**
 * PSU发布标签
 */
public enum PsuTag {
    PREVIEW("PREVIEW", "预览版"),
    FORMAL("FORMAL", "正式版");

    private final String code;
    private final String description;

    PsuTag(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
