package com.example.psu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;    
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC链路追踪配置。
 */
@Configuration
public class WebMvcTraceConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new ControllerTraceInterceptor()).addPathPatterns("/api/**", "/api/v1/**");
    }
}
