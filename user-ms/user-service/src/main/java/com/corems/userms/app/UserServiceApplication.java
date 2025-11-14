package com.corems.userms.app;

import com.corems.common.security.service.TokenProvider;
import com.corems.common.exception.config.EnableCoreMsErrorHandling;
import com.corems.common.logging.EnableCoreMsLogging;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@EnableCoreMsLogging
@EnableCoreMsErrorHandling
public class UserServiceApplication {

    @Bean
    public TokenProvider tokenProvider() {
        return new TokenProvider();
    }

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}