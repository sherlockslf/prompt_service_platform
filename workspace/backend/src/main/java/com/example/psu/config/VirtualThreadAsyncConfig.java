package com.example.psu.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于JDK 21虚拟线程的异步执行配置。
 */
@Configuration
@EnableAsync
public class VirtualThreadAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadAsyncConfig.class);

    @Bean(name = "applicationTaskExecutor", destroyMethod = "close")
    public ExecutorService virtualThreadExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutorBean(ExecutorService virtualThreadExecutorService) {
        return new ConcurrentTaskExecutor(virtualThreadExecutorService);
    }

    @Bean
    public AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler() {
        return new LoggingAsyncExceptionHandler();
    }

    static class LoggingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(
            @NonNull Throwable ex,
            @NonNull Method method,
            @NonNull Object... params
        ) {
            log.error("Async method failed: {}", method == null ? "unknown" : method.getName(), ex);
        }
    }
}
