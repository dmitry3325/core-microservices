package com.corems.communicationms;

import com.corems.common.security.config.EnableCoreMsSecurity;
import com.corems.common.service.exception.config.EnableCommonErrorHandling;
import com.corems.logging.EnableCoreMsLogging;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCoreMsLogging
@EnableCommonErrorHandling
@EnableCoreMsSecurity
public class CommunicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationServiceApplication.class, args);
    }

}
