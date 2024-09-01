package com.corems.common.error.handler.config;

import com.corems.common.error.handler.handler.DefaultErrorComparator;
import com.corems.common.error.handler.handler.DefaultErrorConverter;
import com.corems.common.error.handler.handler.ErrorConverter;
import com.corems.common.error.handler.handler.RestServiceExceptionHandler;
import com.corems.common.error.handler.models.Error;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Comparator;

@Configuration
@PropertySource("classpath:error-handler.properties")
public class ExceptionHandlingConfig {

    @Bean
    @ConditionalOnWebApplication
    public RestServiceExceptionHandler restServiceExceptionHandler(ErrorConverter errorConverter) {
        return new RestServiceExceptionHandler(errorConverter);
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorConverter.class)
    @ConditionalOnWebApplication
    public ErrorConverter errorConverter(Comparator<Error> errorComparator) {
        return new DefaultErrorConverter(errorComparator);
    }

    @Bean
    @ConditionalOnMissingBean(name = "errorComparator")
    @ConditionalOnWebApplication
    public Comparator<Error> defaultErrorComparator() {
        return new DefaultErrorComparator();
    }
}
