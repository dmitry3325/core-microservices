package com.corems.communicationms.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@EnableConfigurationProperties({MailConfig.class, SlackConfig.class, SmsConfig.class})
public class AppConfig {

    public final MailConfig mailConfig;

    public AppConfig(MailConfig mailConfig) {
        this.mailConfig = mailConfig;
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailConfig.getHost());
        mailSender.setPort(mailConfig.getPort());
        mailSender.setUsername(mailConfig.getUsername());
        mailSender.setPassword(mailConfig.getPassword());
        mailSender.setJavaMailProperties(mailConfig.getProperties());
        return mailSender;
    }
}
