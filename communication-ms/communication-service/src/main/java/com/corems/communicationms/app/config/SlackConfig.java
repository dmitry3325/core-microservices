package com.corems.communicationms.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
public record SlackConfig(Boolean enabled, String senderApp, String token) {}
