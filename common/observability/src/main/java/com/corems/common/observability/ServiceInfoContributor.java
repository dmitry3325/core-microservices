package com.corems.common.observability;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Contributes service information to the /actuator/info endpoint.
 */
public class ServiceInfoContributor implements InfoContributor {
    
    private final ObservabilityProperties properties;
    private final Instant startTime;
    
    public ServiceInfoContributor(ObservabilityProperties properties) {
        this.properties = properties;
        this.startTime = Instant.now();
    }
    
    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> serviceInfo = new HashMap<>();
        serviceInfo.put("name", properties.getServiceName());
        serviceInfo.put("version", properties.getServiceVersion());
        serviceInfo.put("environment", properties.getEnvironment());
        serviceInfo.put("startTime", startTime.toString());
        serviceInfo.put("uptime", calculateUptime());
        
        builder.withDetail("service", serviceInfo);
    }
    
    private String calculateUptime() {
        long uptimeSeconds = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}
