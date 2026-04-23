package com.example.psu.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode枚举测试
 */
class ErrorCodeTest {

    @Test
    void testErrorCode_Success() {
        assertEquals(200, ErrorCode.SUCCESS.getCode());
        assertEquals("操作成功", ErrorCode.SUCCESS.getMessage());
    }

    @Test
    void testErrorCode_BadRequest() {
        assertEquals(400, ErrorCode.BAD_REQUEST.getCode());
        assertEquals("请求参数错误", ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    void testErrorCode_NotFound() {
        assertEquals(404, ErrorCode.NOT_FOUND.getCode());
        assertEquals("资源不存在", ErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    void testErrorCode_InternalError() {
        assertEquals(500, ErrorCode.INTERNAL_ERROR.getCode());
        assertEquals("服务器内部错误", ErrorCode.INTERNAL_ERROR.getMessage());
    }

    @Test
    void testErrorCode_PsuNotFound() {
        assertEquals(2001, ErrorCode.PSU_NOT_FOUND.getCode());
        assertEquals("PSU不存在", ErrorCode.PSU_NOT_FOUND.getMessage());
    }

    @Test
    void testErrorCode_PsuAlreadyExists() {
        assertEquals(2002, ErrorCode.PSU_ALREADY_EXISTS.getCode());
        assertEquals("PSU ID已存在", ErrorCode.PSU_ALREADY_EXISTS.getMessage());
    }
}
