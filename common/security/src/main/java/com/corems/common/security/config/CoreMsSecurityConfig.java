package com.corems.common.security.config;

import com.corems.common.security.filter.MdcUserFilter;
import com.corems.common.security.filter.ServiceAuthenticationFilter;
import com.corems.common.security.service.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@ComponentScan("com.corems.common.security")
@PropertySource("classpath:security.properties")
@ConditionalOnProperty(prefix = "spring.security.common", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CoreMsSecurityConfig {

    private final MdcUserFilter mdcUserFilter;
    private final TokenProvider tokenProvider;

    @Value("${spring.security.white-list-urls:/actuator/health}")
    private String[] whiteListUrls;

    @Bean
    public ServiceAuthenticationFilter serviceAuthenticationFilter() {
        return new ServiceAuthenticationFilter(tokenProvider);
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity httpSecurity,
                                         ObjectProvider<ServiceAuthenticationFilter> serviceAuthenticationFilterProvider) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        httpSecurity.cors(cors -> {});
        httpSecurity.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        httpSecurity.formLogin(AbstractHttpConfigurer::disable);
        httpSecurity.logout(AbstractHttpConfigurer::disable);
        httpSecurity.rememberMe(AbstractHttpConfigurer::disable);
        httpSecurity.httpBasic(AbstractHttpConfigurer::disable);

        httpSecurity.authorizeHttpRequests(
                auth -> auth
                        .requestMatchers(whiteListUrls).permitAll()
                        .anyRequest()
                        .authenticated()
        );

        ServiceAuthenticationFilter saf = serviceAuthenticationFilterProvider.getIfAvailable();
        if (saf != null) {
            httpSecurity.addFilterAfter(saf, CsrfFilter.class);
            httpSecurity.addFilterAfter(mdcUserFilter, ServiceAuthenticationFilter.class);
        } else {
            httpSecurity.addFilterAfter(mdcUserFilter, CsrfFilter.class);
        }

        log.info("Security configuration completed (service-auth-filter present={})", saf != null);

        return httpSecurity.build();
    }
}
