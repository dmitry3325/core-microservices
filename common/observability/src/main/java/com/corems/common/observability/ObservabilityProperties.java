package com.corems.common.observability;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for observability features.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "corems.observability")
public class ObservabilityProperties {
    
    /**
     * Service name for identification in metrics and logs.
     */
    private String serviceName = "unknown-service";
    
    /**
     * Service version.
     */
    private String serviceVersion = "0.0.1-SNAPSHOT";
    
    /**
     * Environment (dev, staging, prod).
     */
    private String environment = "dev";
    
    /**
     * Enable detailed health information.
     */
    private boolean detailedHealth = true;
}
