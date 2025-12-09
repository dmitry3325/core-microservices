package com.corems.common.logging;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration to register the correlation id filter early in the chain.
 * It will be created unless another bean of the same type exists.
 */
@Configuration
@ConditionalOnProperty(prefix = "corems.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<Filter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>(filter);
        // High precedence to run before security filters like ServiceAuthenticationFilter
        reg.setOrder(Integer.MIN_VALUE + 10);
        reg.addUrlPatterns("/*");
        return reg;
    }

}
