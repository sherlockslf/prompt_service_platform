package com.example.psu.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.psu.dto.UserCreateRequest;
import com.example.psu.dto.response.UserResponse;
import com.example.psu.entity.User;
import com.example.psu.enums.UserRole;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.UserService;

import jakarta.validation.Valid;
import java.util.Objects;

/**
 * 用户管理控制器（仅管理员使用）
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    @Autowired
    private AsyncDispatchService asyncDispatchService;
    
    /**
     * 查询用户列表。
     * 请求方法与路径：GET /api/users（兼容 /api/v1/users）。
     * 入参：无。
     * 返回：UserResponse 列表。
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 创建用户（同步）。
     * 请求方法与路径：POST /api/users（兼容 /api/v1/...）。
     * 入参：UserCreateRequest（username、password、role、enabled）。
     * 返回：创建后的 UserResponse。
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        request = RequestValidationUtils.requireNonNull(request, "request");
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(UserRole.fromCode(request.getRole().toUpperCase()));
        user.setEnabled(request.getEnabled());
        
        User createdUser = userService.createUser(user);
        UserResponse response = convertToResponse(createdUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建用户（异步）。
     * 请求方法与路径：POST /api/users/async（兼容 /api/v1/...）。
     * 入参：UserCreateRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/async")
    public ResponseEntity<String> createUserAsync(@Valid @RequestBody UserCreateRequest request) {
        asyncDispatchService.dispatch(() -> {
            User payload = new User();
            payload.setUsername(request.getUsername());
            payload.setPassword(request.getPassword());
            payload.setRole(UserRole.fromCode(request.getRole().toUpperCase()));
            payload.setEnabled(request.getEnabled());
            userService.createUser(payload);
        });
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 切换用户启用状态（同步）。
     * 请求方法与路径：PUT /api/users/by-id/toggle-status（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：更新后的 UserResponse。
     */
    @PostMapping("/by-id/toggle-status")
    public ResponseEntity<UserResponse> toggleUserStatus(@RequestParam Long id) {
        RequestValidationUtils.requireNonNull(id, "id");
        User user = userService.toggleUserStatus(id);
        UserResponse response = convertToResponse(user);
        return ResponseEntity.ok(response);
    }

    /**
     * 切换用户启用状态（异步）。
     * 请求方法与路径：PUT /api/users/by-id/toggle-status/async（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-id/toggle-status/async")
    public ResponseEntity<String> toggleUserStatusAsync(@RequestParam Long id) {
        asyncDispatchService.dispatch(() -> userService.toggleUserStatus(id));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 将用户实体转换为响应DTO
     */
    private UserResponse convertToResponse(User user) {
        user = RequestValidationUtils.requireNonNull(user, "user");
        User safeUser = Objects.requireNonNull(user);
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(safeUser, response);
        response.setRole(safeUser.getRole().getCode());
        return response;
    }
}





