package com.example.psu.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 控制器方法级追踪日志。
 */
public class ControllerTraceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ControllerTraceInterceptor.class);
    private static final String START_NANO_KEY = "controllerStartNano";

    @Override
    public boolean preHandle(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler
    ) {
        request.setAttribute(START_NANO_KEY, System.nanoTime());
        if (handler instanceof HandlerMethod method) {
            log.info(
                "controllerEnter path={} method={} handler={}#{} thread={} virtualThread={}",
                request.getRequestURI(),
                request.getMethod(),
                method.getBeanType().getSimpleName(),
                method.getMethod().getName(),
                Thread.currentThread().getName(),
                Thread.currentThread().isVirtual()
            );
        }
        return true;
    }

    @Override
    public void afterCompletion(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler,
        @Nullable Exception ex
    ) {
        Object begin = request.getAttribute(START_NANO_KEY);
        long startNano = begin instanceof Long ? (Long) begin : System.nanoTime();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
        if (ex == null) {
            log.info(
                "controllerExit path={} method={} status={} durationMs={}",
                request.getRequestURI(),
                request.getMethod(),
                response.getStatus(),
                elapsed
            );
        } else {
            log.error(
                "controllerError path={} method={} status={} durationMs={} error={}",
                request.getRequestURI(),
                request.getMethod(),
                response.getStatus(),
                elapsed,
                ex.getMessage(),
                ex
            );
        }
    }
}
