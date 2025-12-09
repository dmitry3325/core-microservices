package com.corems.common.observability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for observability features.
 * Provides unified health checks, metrics, and monitoring for all services.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityAutoConfiguration {

    @Bean
    public ServiceInfoContributor serviceInfoContributor(ObservabilityProperties properties) {
        return new ServiceInfoContributor(properties);
    }
}
