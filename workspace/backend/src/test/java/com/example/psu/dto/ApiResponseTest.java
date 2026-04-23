package com.example.psu.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiResponse测试
 */
class ApiResponseTest {

    @Test
    void testSuccess_WithData() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);
        
        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("test data", response.getData());
    }

    @Test
    void testSuccess_WithMessageAndData() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success("操作成功", data);
        
        assertEquals(200, response.getCode());
        assertEquals("操作成功", response.getMessage());
        assertEquals("test data", response.getData());
    }

    @Test
    void testError_WithCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.error(400, "请求参数错误");
        
        assertEquals(400, response.getCode());
        assertEquals("请求参数错误", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testError_WithData() {
        String errorData = "error details";
        ApiResponse<String> response = ApiResponse.error(500, "服务器错误", errorData);
        
        assertEquals(500, response.getCode());
        assertEquals("服务器错误", response.getMessage());
        assertEquals("error details", response.getData());
    }
}
