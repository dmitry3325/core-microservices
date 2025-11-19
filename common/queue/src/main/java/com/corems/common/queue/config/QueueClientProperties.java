package com.corems.common.queue.config;

public interface QueueClientProperties {
    int getRetryCount();
    long getPollIntervalMs();
    String getDefaultQueue();
}
