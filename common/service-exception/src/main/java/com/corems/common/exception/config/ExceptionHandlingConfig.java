package com.corems.common.exception.config;

import com.corems.common.exception.filter.ExceptionHandlingFilter;
import com.corems.common.exception.handler.DefaultErrorComparator;
import com.corems.common.exception.handler.DefaultErrorConverter;
import com.corems.common.exception.handler.ErrorConverter;
import com.corems.common.exception.handler.RestServiceExceptionHandler;
import com.corems.common.exception.model.Error;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
    @ConditionalOnWebApplication
    public FilterRegistrationBean<ExceptionHandlingFilter> exceptionHandlingFilter(RestServiceExceptionHandler exceptionHandler) {
        FilterRegistrationBean<ExceptionHandlingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ExceptionHandlingFilter(exceptionHandler));
        registrationBean.setOrder(Integer.MIN_VALUE); // Set the highest precedence
        return registrationBean;
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
