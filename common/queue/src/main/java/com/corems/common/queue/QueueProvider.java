package com.corems.common.queue;

import com.corems.common.queue.config.QueueProperties;
import com.corems.common.exception.ServiceException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class QueueProvider {
    @Getter
    private final QueueProperties queueProperties;
    private final Map<SupportedQueueProvider, QueueClient> providers = new ConcurrentHashMap<>();

    public QueueProvider(QueueProperties queueProperties) {
        this.queueProperties = Objects.requireNonNull(queueProperties, "queueProperties must not be null");
    }

    /**
     * Return whether queueing is enabled via configuration.
     */
    public boolean isEnabled() {
        return queueProperties.isEnabled() && !providers.isEmpty();
    }

    /**
     * Return the configured default QueueClient according to the bound properties.
     * If queueing is disabled, throws ServiceException.
     * If no provider is registered for the configured provider name, a ServiceException is thrown.
     */
    public QueueClient getDefaultClient() {
        if (!isEnabled()) {
            throw new ServiceException("Queueing is disabled by configuration");
        }

        SupportedQueueProvider prov = queueProperties.getProvider();
        if (prov == null) throw new ServiceException("Queue provider is not configured");

        return getProvider(prov);
    }

    /**
     * Register a provider by enum key.
     */
    public void registerProvider(SupportedQueueProvider providerType, QueueClient client) {
        providers.put(Objects.requireNonNull(providerType, "providerType must not be null"), Objects.requireNonNull(client, "client must not be null"));
        log.info("Registered QueueClient for provider type: {}", providerType);
    }

    /**
     * Lookup by enum key.
     */
    public QueueClient getProvider(SupportedQueueProvider providerType) {
        QueueClient client = providers.get(providerType);
        if (client == null) {
            throw new ServiceException("No QueueClient registered for provider: '" + providerType + "'");
        }
        return client;
    }
}
