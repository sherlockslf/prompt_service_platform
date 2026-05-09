package com.example.psu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.psu.dto.AuthRequest;
import com.example.psu.dto.AuthResponse;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.UserService;

import jakarta.validation.Valid;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    @Autowired
    private AsyncDispatchService asyncDispatchService;
    
    /**
     * 用户登录并签发访问令牌。
     * 请求方法与路径：POST /api/auth/login（兼容 /api/v1/auth/login）。
     * 入参：AuthRequest（username、password）。
     * 返回：AuthResponse（token、用户信息、角色等）。
     * 说明：失败时由统一异常处理器返回标准错误结构。
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = userService.authenticate(authRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 异步触发登录鉴权流程。
     * 请求方法与路径：POST /api/auth/login/async（兼容 /api/v1/auth/login/async）。
     * 入参：AuthRequest（username、password）。
     * 返回：202 ACCEPTED，表示任务已提交到异步执行器。
     * 说明：该接口仅用于异步化场景；登录结果需通过后续机制获取。
     */
    @PostMapping("/login/async")
    public ResponseEntity<String> loginAsync(@Valid @RequestBody AuthRequest authRequest) {
        asyncDispatchService.dispatch(() -> userService.authenticate(authRequest));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 获取当前登录用户资料。
     * 请求方法与路径：GET /api/auth/profile（兼容 /api/v1/auth/profile）。
     * 入参：无（从 SecurityContext 读取当前用户）。
     * 返回：AuthResponse（当前用户基础信息与权限信息）。
     */
    @GetMapping("/profile")
    public ResponseEntity<AuthResponse> getProfile() {
        AuthResponse response = userService.getCurrentUser(null);
        return ResponseEntity.ok(response);
    }
}




