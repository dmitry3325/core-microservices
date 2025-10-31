package com.corems.common.security.config;

import com.corems.common.security.filter.MdcUserFilter;
import com.corems.common.security.filter.ServiceAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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
@PropertySource("classpath:security.properties")
public class CoreMsSecurityConfig {

    private final ServiceAuthenticationFilter serviceAuthenticationFilter;
    private final MdcUserFilter mdcUserFilter;

    @Value("${spring.security.white-list-urls:/actuator/health}")
    private String[] whiteListUrls;

    @Bean
    public SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
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


        httpSecurity.addFilterAfter(serviceAuthenticationFilter, CsrfFilter.class);
        // Place MDC user filter after authentication so user id is available in MDC
        httpSecurity.addFilterAfter(mdcUserFilter, ServiceAuthenticationFilter.class);

        log.info("Security configuration completed");

        return httpSecurity.build();
    }
}
