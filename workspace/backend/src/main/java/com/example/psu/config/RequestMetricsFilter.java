package com.example.psu.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 轻量请求指标日志：耗时与状态码。
 */
@Component
public class RequestMetricsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestMetricsFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String START_NANO_KEY = "requestStartNano";

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        request.setAttribute(START_NANO_KEY, System.nanoTime());
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            Object begin = request.getAttribute(START_NANO_KEY);
            long startNano = begin instanceof Long ? (Long) begin : System.nanoTime();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
            log.info(
                "requestDone requestId={} path={} method={} status={} durationMs={} thread={} virtualThread={}",
                requestId,
                request.getRequestURI(),
                request.getMethod(),
                response.getStatus(),
                elapsedMs,
                Thread.currentThread().toString(),
                Thread.currentThread().isVirtual()
            );
            MDC.remove(REQUEST_ID_KEY);
        }
    }
}
