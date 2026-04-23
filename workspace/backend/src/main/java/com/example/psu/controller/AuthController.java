package com.example.psu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.psu.dto.AuthRequest;
import com.example.psu.dto.AuthResponse;
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
    
    /**
     * 登录页面执行用户登录操作
     * 参数：username-用户名，password-密码
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = userService.authenticate(authRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 全局导航栏获取当前登录用户信息
     * 参数：无（从SecurityContext自动获取）
     */
    @GetMapping("/profile")
    public ResponseEntity<AuthResponse> getProfile() {
        AuthResponse response = userService.getCurrentUser(null);
        return ResponseEntity.ok(response);
    }
}
