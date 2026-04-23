package com.example.psu.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户响应DTO
 */
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String role;
    private Boolean enabled;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
