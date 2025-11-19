package com.corems.common.queue;

import com.corems.common.queue.config.QueueClientProperties;

import java.util.Optional;

public interface QueueClient {
    /**
     * Get the properties used to configure this client.
     */
    QueueClientProperties getProperties();

    /**
     * Send a message to the default destination (exchange/routing configured in properties).
     */
    void send(QueueMessage message);

    /**
     * Send a message to a specific destination (exchange/routingKey or queue/topic depending on provider).
     */
    void send(String destination, QueueMessage message);

    /**
     * Poll a destination for a single message synchronously with timeout from default queue from config.
     * Returns Optional.empty() when no message is available within timeout.
     *
     */
    Optional<QueueMessage> poll();

    /**
     * Poll a destination for a single message synchronously with timeout.
     * Returns Optional.empty() when no message is available within timeout.
     *
     * The destination parameter is provider-specific: it can be a queue name, routing key, or topic.
     */
    Optional<QueueMessage> poll(String destination);
}
