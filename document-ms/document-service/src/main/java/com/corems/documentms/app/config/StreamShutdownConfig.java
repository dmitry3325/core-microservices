package com.corems.documentms.app.config;

import com.corems.documentms.app.util.StreamResponseHelper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a DisposableBean that gracefully shuts down StreamResponseHelper's copier executor
 * when the Spring application context is closing.
 */
@Configuration
public class StreamShutdownConfig {

    @Bean
    public DisposableBean streamResponseHelperShutdown() {
        return StreamResponseHelper::shutdown;
    }
}

