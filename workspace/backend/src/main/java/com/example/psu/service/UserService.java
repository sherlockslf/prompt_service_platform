package com.example.psu.service;

import com.example.psu.dto.AuthRequest;
import com.example.psu.dto.AuthResponse;
import com.example.psu.entity.User;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.UserRepository;
import com.example.psu.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 用户服务
 */
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 用户登录认证
     * @param authRequest 认证请求
     * @return 认证响应
     */
    public AuthResponse authenticate(AuthRequest authRequest) {
        authRequest = RequestValidationUtils.requireNonNull(authRequest, "authRequest");
        RequestValidationUtils.requireNonBlank(authRequest.getUsername(), "username");
        RequestValidationUtils.requireNonBlank(authRequest.getPassword(), "password");
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authRequest.getUsername(),
                    authRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
            
            return new AuthResponse(token, user.getUsername(), user.getRole().name());
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有用户列表
     * @return 用户列表
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * 创建用户（仅管理员使用）
     * @param user 用户实体
     * @return 创建的用户
     */
    public User createUser(User user) {
        user = RequestValidationUtils.requireNonNull(user, "user");
        RequestValidationUtils.requireNonBlank(user.getUsername(), "username");
        RequestValidationUtils.requireNonBlank(user.getPassword(), "password");
        RequestValidationUtils.requireNonNull(user.getRole(), "role");
        RequestValidationUtils.requireNonNull(user.getEnabled(), "enabled");
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // 创建用户时统一加密密码，避免明文入库。
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    /**
     * 切换用户启用状态
     * @param id 用户ID
     * @return 更新后的用户
     */
    public User toggleUserStatus(Long id) {
        RequestValidationUtils.requireNonNull(id, "id");
        Long safeId = Objects.requireNonNull(id);
        User user = userRepository.findById(safeId)
                .orElseThrow(() -> new RuntimeException("User not found: " + safeId));
        user.setEnabled(!user.getEnabled());
        return userRepository.save(user);
    }
    
    /**
     * 获取当前用户信息
     * @param authentication 当前认证信息
     * @return 当前用户信息
     */
    public AuthResponse getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
            return new AuthResponse(token, user.getUsername(), user.getRole().name());
        }

        User fallback = userRepository.findAll().stream().findFirst()
            .orElse(null);
        if (fallback != null) {
            return new AuthResponse("", fallback.getUsername(), fallback.getRole().name());
        }
        return new AuthResponse("", "guest", "BUSINESS");
    }
}


