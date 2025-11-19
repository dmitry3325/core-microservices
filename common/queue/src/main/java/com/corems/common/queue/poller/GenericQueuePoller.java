package com.corems.common.queue.poller;

import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class GenericQueuePoller implements AutoCloseable {

    private final QueueClient queueClient;
    private final Map<String, MessageHandler> handlers;
    private final String destination;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService pollExecutor;

    public GenericQueuePoller(QueueClient queueClient, Map<String, MessageHandler> handlers) {
        this(queueClient, handlers, Collections.emptyMap(), true);
    }

    public GenericQueuePoller(QueueClient queueClient, Map<String, MessageHandler> handlers, Map<String, String> defaultDestination) {
        this(queueClient, handlers, defaultDestination, true);
    }

    public GenericQueuePoller(QueueClient queueClient, Map<String, MessageHandler> handlers, Map<String, String> defaultDestination, boolean autoStart) {
        this.queueClient = queueClient;
        this.handlers = handlers;
        this.destination = queueClient.getProperties().getDefaultQueue();
        this.pollExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "generic-queue-poller");
            t.setDaemon(true);
            return t;
        });

        if (autoStart) start();
    }

    /**
     * Start the background poll loop. Safe to call multiple times (idempotent).
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            pollExecutor.submit(this::runLoop);
            log.info("GenericQueuePoller started for destination={}", destination);
        }
    }

    private void runLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                pollOnce();
            } catch (Exception ex) {
                // Generic protection: log and backoff briefly to avoid hot-loop on persistent errors
                log.error("Unexpected error in GenericQueuePoller loop", ex);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("GenericQueuePoller loop exiting for destination={}", destination);
    }

    /**
     * Stop the background poll loop and shutdown the executor. This method blocks briefly while waiting for termination.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                pollExecutor.shutdownNow();
                if (!pollExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("GenericQueuePoller executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("GenericQueuePoller stopped for destination={}", destination);
        }
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Single poll iteration: performs one blocking poll and processes the message if present.
     */
    public void pollOnce() {
        try {
            Optional<QueueMessage> maybe = queueClient.poll(destination);
            if (maybe.isEmpty()) {
                return;
            }

            QueueMessage qm = maybe.get();
            processMessage(qm);
        } catch (Exception e) {
            log.error("Unexpected error while polling queue", e);
        }
    }

    protected void processMessage(QueueMessage qm) {
        try {
            MessageHandler handler = handlers.get(qm.getType());
            if (handler == null) {
                log.error("No handler registered for message type={}", qm.getType());
                return;
            }
            handler.handle(qm);
        } catch (Exception ex) {
            log.error("Failed handling message id={}: {}", qm.getId(), ex.getMessage());
            qm.setAttempts(qm.getAttempts() + 1);
            int retryCount = queueClient.getProperties().getRetryCount();
            if (retryCount > qm.getAttempts()) {
                try {
                    queueClient.send(destination, qm);
                    log.info("Re-enqueued message id={} for retry (destination={})", qm.getId(), destination);
                } catch (Exception e) {
                    log.error("Failed to re-enqueue message id={}", qm.getId(), e);
                }
            } else {
                log.warn("Dropping message id={} after {} attempts", qm.getId(), qm.getAttempts());
            }
        }
    }
}
