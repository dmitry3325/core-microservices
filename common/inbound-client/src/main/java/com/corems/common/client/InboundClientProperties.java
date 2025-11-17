package com.corems.common.client;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "corems.client")
public class InboundClientProperties {

    private final int defaultTimeoutSeconds = 10;
}

