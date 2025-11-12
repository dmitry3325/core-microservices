package com.corems.common.queue;

/**
 * Enum of supported queue providers. Only RabbitMQ is supported for now.
 */
public enum SupportedQueueProvider {
    RABBIT_MQ;

    public static SupportedQueueProvider fromString(String s) {
        if (s == null) return null;
        String v = s.trim().toLowerCase();
        return switch (v) {
            case "rabbitmq", "rabbit_mq", "rabbit-mq", "rabbit" -> RABBIT_MQ;
            default -> null;
        };
    }

}
