package com.corems.common.queue;

import java.util.Optional;

public interface QueueClient {


    /**
     * Send a message to the default destination (exchange/routing configured in properties).
     */
    <T> void send(QueueMessage<T> message);

    /**
     * Send a message to a specific destination (exchange/routingKey or queue/topic depending on provider).
     */
    <T> void send(String destination, QueueMessage<T> message);

    /**
     * Poll a destination for a single message synchronously with timeout.
     * Returns Optional.empty() when no message is available within timeout.
     *
     * The destination parameter is provider-specific: it can be a queue name, routing key, or topic.
     */
    <T> Optional<QueueMessage<T>> poll(String destination);
}
