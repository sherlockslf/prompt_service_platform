package com.example.psu.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ChatServiceThreadPool {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceThreadPool.class);

    private final ExecutorService executor;
    private final boolean virtualEnabled;
    private final String threadNamePrefix;
    private final AtomicLong taskSeq = new AtomicLong(0);

    public ChatServiceThreadPool(String threadNamePrefix,
                                 boolean virtualEnabled,
                                 int corePoolSize,
                                 int maximumPoolSize,
                                 long keepAliveTime,
                                 TimeUnit unit,
                                 BlockingQueue<Runnable> workQueue) {
        this.virtualEnabled = virtualEnabled;
        this.threadNamePrefix = threadNamePrefix;

        if (virtualEnabled) {
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            log.info("THREADING event=pool_init component=ChatServiceThreadPool pool={} mode=virtual", threadNamePrefix);
        } else {
            ThreadFactory factory = new CustomThreadFactory(threadNamePrefix);
            this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
            log.info("THREADING event=pool_init component=ChatServiceThreadPool pool={} mode=platform core={} max={} queueCapacity={}",
                threadNamePrefix, corePoolSize, maximumPoolSize, workQueue.remainingCapacity() + workQueue.size());
        }
    }

    public static ChatServiceThreadPool createDefault(String threadNamePrefix,
                                                      boolean virtualEnabled,
                                                      int corePoolSize,
                                                      int maximumPoolSize,
                                                      long keepAliveSeconds,
                                                      int queueCapacity) {
        return new ChatServiceThreadPool(
            threadNamePrefix,
            virtualEnabled,
            corePoolSize,
            maximumPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity)
        );
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public <T> Future<T> submit(Callable<T> task) {
        long taskId = taskSeq.incrementAndGet();
        log.debug("THREADING event=task_submit component=ChatServiceThreadPool pool={} mode={} taskId={}",
            threadNamePrefix, currentMode(), taskId);
        return executor.submit(() -> {
            try {
                T result = task.call();
                log.debug("THREADING event=task_done component=ChatServiceThreadPool pool={} mode={} taskId={}",
                    threadNamePrefix, currentMode(), taskId);
                return result;
            } catch (Exception e) {
                log.error("THREADING event=task_failed component=ChatServiceThreadPool pool={} mode={} taskId={}",
                    threadNamePrefix, currentMode(), taskId, e);
                throw e;
            }
        });
    }

    public Future<?> submit(Runnable task) {
        return submit(() -> {
            task.run();
            return null;
        });
    }

    public void shutdown() {
        log.info("THREADING event=pool_shutdown component=ChatServiceThreadPool pool={} mode={} submittedTasks={}",
            threadNamePrefix, currentMode(), taskSeq.get());
        executor.shutdown();
    }

    public String currentMode() {
        return virtualEnabled ? "virtual" : "platform";
    }

    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        CustomThreadFactory(String namePrefix) {
            this.namePrefix = (namePrefix == null || namePrefix.isBlank()) ? "pool-thread-" : namePrefix + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}

