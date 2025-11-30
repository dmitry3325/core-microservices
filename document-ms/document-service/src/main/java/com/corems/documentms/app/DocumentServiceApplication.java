package com.corems.documentms.app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class DocumentServiceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DocumentServiceApplication.class)
                .logStartupInfo(true)
                .run(args);
    }
}

