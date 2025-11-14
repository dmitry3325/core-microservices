package com.corems.communicationms.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms")
public record SmsConfig(Boolean enabled, String accountSid, String authToken, String fromNumber) {}
