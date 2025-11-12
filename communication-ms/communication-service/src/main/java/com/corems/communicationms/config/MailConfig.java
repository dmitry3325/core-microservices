package com.corems.communicationms.config;

import java.util.Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(ObjectProvider<MailProperties> propsProvider) {
        MailProperties props = propsProvider.getIfAvailable(() -> new MailProperties());

        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // only apply properties when present; fall back to sane defaults
        if (props.getHost() != null) {
            sender.setHost(props.getHost());
        } else {
            sender.setHost("localhost");
        }

        if (props.getPort() != null && props.getPort() > 0) {
            sender.setPort(props.getPort());
        } else {
            sender.setPort(25);
        }

        if (props.getUsername() != null) {
            sender.setUsername(props.getUsername());
        }
        if (props.getPassword() != null) {
            sender.setPassword(props.getPassword());
        }
        if (props.getProtocol() != null) {
            sender.setProtocol(props.getProtocol());
        }

        Properties jprops = new Properties();
        if (props.getProperties() != null) {
            jprops.putAll(props.getProperties());
        }
        sender.setJavaMailProperties(jprops);
        return sender;
    }
}