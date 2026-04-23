package com.example.psu.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常基类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    
    private int code;
    private String message;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }
}
