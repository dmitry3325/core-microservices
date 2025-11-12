package com.corems.common.queue.config;

import com.corems.common.queue.SupportedQueueProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "queue")
@Validated
@Setter
@Getter
public class QueueProperties {

    private boolean enabled = false;
    private SupportedQueueProvider provider = SupportedQueueProvider.RABBIT_MQ;

    private Providers providers = new Providers();

    @Getter
    @Setter
    public static class Providers {
        private RabbitMqProperties rabbitMq = new RabbitMqProperties();
    }

    @Getter
    @Setter
    public static class RabbitMqProperties {
        private String exchange = "";
        private String defaultQueue = "";
        private String routingKey = "";
        private String host = "localhost";
        private int port = 5672;
        private String username = "";
        private String password = "";
        private long pollTimeoutMs = 5000L;
    }
}
