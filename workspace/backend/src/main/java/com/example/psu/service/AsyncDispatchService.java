package com.example.psu.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * 控制器层统一异步派发器。
 */
@Service
public class AsyncDispatchService {

    private static final Logger log = LoggerFactory.getLogger(AsyncDispatchService.class);
    private final AsyncTaskExecutor taskExecutor;

    public AsyncDispatchService(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void dispatch(Runnable task) {
        log.info("asyncDispatch submitted thread={} virtualThread={}", Thread.currentThread().getName(), Thread.currentThread().isVirtual());
        taskExecutor.submit(() -> {
            long start = System.currentTimeMillis();
            try {
                log.info("asyncDispatch started thread={} virtualThread={}", Thread.currentThread().getName(), Thread.currentThread().isVirtual());
                task.run();
                log.info("asyncDispatch completed durationMs={}", System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.error("asyncDispatch failed durationMs={} error={}", System.currentTimeMillis() - start, ex.getMessage(), ex);
                throw ex;
            }
        });
    }
}
