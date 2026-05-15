package com.example.psu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.threading")
public class AppThreadingProperties {

    private Virtual virtual = new Virtual();
    private Blocking blocking = new Blocking();

    public Virtual getVirtual() {
        return virtual;
    }

    public void setVirtual(Virtual virtual) {
        this.virtual = virtual;
    }

    public Blocking getBlocking() {
        return blocking;
    }

    public void setBlocking(Blocking blocking) {
        this.blocking = blocking;
    }

    public static class Virtual {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Blocking {
        private String executor = "virtual";
        private int corePoolSize = 50;
        private int maximumPoolSize = 200;
        private int queueCapacity = 100;
        private long keepAliveSeconds = 60;

        public String getExecutor() {
            return executor;
        }

        public void setExecutor(String executor) {
            this.executor = executor;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public long getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(long keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }
}

