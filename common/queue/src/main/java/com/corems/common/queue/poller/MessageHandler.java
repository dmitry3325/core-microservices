package com.corems.common.queue.poller;

import com.corems.common.queue.QueueMessage;

/**
 * Minimal, non-generic handler that accepts a QueueMessage with Object payload.
 * Using a plain Object payload simplifies runtime casting and avoids excessive generics across common code.
 */
@FunctionalInterface
public interface MessageHandler {
    void handle(QueueMessage message);
}
