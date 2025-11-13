package com.corems.communicationms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mail")
public class MailConfig {
    private Boolean enabled;
    private String defaultFrom;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String protocol;
    private Properties properties = new Properties();
}
