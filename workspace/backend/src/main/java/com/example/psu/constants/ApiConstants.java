package com.example.psu.constants;

/**
 * API相关常量
 */
public class ApiConstants {
    
    /**
     * API路径常量
     */
    public static final String API_BASE_PATH = "/api";
    public static final String API_AUTH_PATH = "/api/auth";
    public static final String API_PSUS_PATH = "/api/psus";
    public static final String API_SCHEMAS_PATH = "/api/schemas";
    public static final String API_PROMPTS_PATH = "/api/prompts";
    public static final String API_VERSIONS_PATH = "/api/versions";
    public static final String API_USERS_PATH = "/api/users";
    public static final String API_CONFIGS_PATH = "/api/configs";
    public static final String API_AUDIT_LOGS_PATH = "/api/audit-logs";
    public static final String API_TEST_DATASETS_PATH = "/api/test-datasets";
    
    /**
     * HTTP状态码
     */
    public static final int HTTP_SUCCESS = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_INTERNAL_ERROR = 500;
    
    /**
     * 响应消息
     */
    public static final String MSG_SUCCESS = "success";
    public static final String MSG_CREATED = "创建成功";
    public static final String MSG_UPDATED = "更新成功";
    public static final String MSG_DELETED = "删除成功";
    public static final String MSG_NOT_FOUND = "资源不存在";
    public static final String MSG_BAD_REQUEST = "请求参数错误";
    public static final String MSG_UNAUTHORIZED = "未授权访问";
    public static final String MSG_FORBIDDEN = "权限不足";
    public static final String MSG_INTERNAL_ERROR = "服务器内部错误";
    
    private ApiConstants() {
        // 防止实例化
    }
}
