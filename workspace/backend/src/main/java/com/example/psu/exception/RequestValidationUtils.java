package com.example.psu.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请求参数校验工具
 */
public final class RequestValidationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestValidationUtils.class);

    private RequestValidationUtils() {
    }

    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            // 统一记录参数缺失日志，便于排查调用方问题。
            LOGGER.warn("请求参数为空: {}", fieldName);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求参数不能为空: " + fieldName);
        }
        return value;
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            // 统一记录空字符串参数，避免业务流程进入不可控状态。
            LOGGER.warn("请求参数为空白: {}", fieldName);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求参数不能为空: " + fieldName);
        }
        return value;
    }
}
