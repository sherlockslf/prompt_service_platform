package com.example.psu.config;

import com.example.psu.util.ChatServiceThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * 基于线程池组件的异步执行配置。
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(AppThreadingProperties.class)
public class VirtualThreadAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadAsyncConfig.class);

    @Bean(destroyMethod = "shutdown")
    public ChatServiceThreadPool chatServiceThreadPool(AppThreadingProperties props) {
        boolean virtualEnabled = props.getVirtual().isEnabled()
            && "virtual".equalsIgnoreCase(props.getBlocking().getExecutor());

        ChatServiceThreadPool pool = ChatServiceThreadPool.createDefault(
            "psu-async",
            virtualEnabled,
            props.getBlocking().getCorePoolSize(),
            props.getBlocking().getMaximumPoolSize(),
            props.getBlocking().getKeepAliveSeconds(),
            props.getBlocking().getQueueCapacity()
        );
        log.info("THREADING event=config_applied pool=psu-async mode={}", pool.currentMode());
        return pool;
    }

    @Bean(name = "applicationTaskExecutor")
    public ExecutorService virtualThreadExecutorService(ChatServiceThreadPool chatServiceThreadPool) {
        return chatServiceThreadPool.getExecutor();
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
