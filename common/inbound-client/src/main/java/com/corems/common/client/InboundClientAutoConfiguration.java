package com.corems.common.client;

import com.corems.common.logging.CorrelationIdFilter;
import com.corems.common.security.UserPrincipal;
import com.corems.common.security.service.TokenProvider;
import com.corems.common.security.CoreMsRoles;
import io.netty.channel.ChannelOption;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;

@AutoConfiguration
@EnableConfigurationProperties(InboundClientProperties.class)
public class InboundClientAutoConfiguration {

    @Bean("inboundCorrelationIdFilter")
    @ConditionalOnMissingBean(name = "inboundCorrelationIdFilter")
    public ExchangeFilterFunction inboundCorrelationIdFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            ClientRequest modifiedRequest = ClientRequest.from(request)
                    .header(CorrelationIdFilter.HEADER_X_CORRELATION_ID, correlationId)
                    .build();

            return Mono.just(modifiedRequest);
        });
    }

    @Bean
    @ConditionalOnMissingBean(name = "bearerTokenFilter")
    public ExchangeFilterFunction bearerTokenFilter(TokenProvider tokenProvider) {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            String token;
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UserPrincipal principal) {

                Map<String, Object> claims = new HashMap<>();
                claims.put(TokenProvider.CLAIM_EMAIL, principal.getEmail());
                claims.put(TokenProvider.CLAIM_FIRST_NAME, principal.getFirstName());
                claims.put(TokenProvider.CLAIM_LAST_NAME, principal.getLastName());
                claims.put(TokenProvider.CLAIM_USER_ID, principal.getUserId().toString());
                claims.put(TokenProvider.CLAIM_ROLES, principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());

                token = tokenProvider.createAccessToken(principal.getTokenId().toString(), claims);
            } else {
                Map<String, Object> claims = new HashMap<>();
                claims.put(TokenProvider.CLAIM_ROLES, List.of(CoreMsRoles.SYSTEM));

                token = tokenProvider.createAccessToken(null, claims);
            }

            ClientRequest modifiedRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer " + token)
                    .build();

            return Mono.just(modifiedRequest);
        });
    }

    @Bean
    @ConditionalOnMissingBean(name = "inboundWebClientBuilder")
    public WebClient.Builder inboundWebClientBuilder(
            @Qualifier("inboundCorrelationIdFilter") ExchangeFilterFunction correlationIdFilter,
            @Qualifier("bearerTokenFilter") ExchangeFilterFunction bearerTokenFilter,
            InboundClientProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getDefaultTimeoutSeconds())
                .responseTimeout(Duration.ofSeconds(props.getDefaultTimeoutSeconds()));

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        return WebClient.builder()
                .clientConnector(connector)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter);
    }

    @Bean
    @ConditionalOnMissingBean(name = "inboundRestClientBuilder")
    public RestClient.Builder inboundRestClientBuilder(TokenProvider tokenProvider) {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    // Add correlation ID
                    String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
                    if (correlationId == null || correlationId.isBlank()) {
                        correlationId = UUID.randomUUID().toString();
                    }
                    request.getHeaders().add(CorrelationIdFilter.HEADER_X_CORRELATION_ID, correlationId);

                    // Add bearer token
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    String token;
                    if (authentication != null && authentication.isAuthenticated()
                            && authentication.getPrincipal() instanceof UserPrincipal principal) {

                        Map<String, Object> claims = new HashMap<>();
                        claims.put(TokenProvider.CLAIM_EMAIL, principal.getEmail());
                        claims.put(TokenProvider.CLAIM_FIRST_NAME, principal.getFirstName());
                        claims.put(TokenProvider.CLAIM_LAST_NAME, principal.getLastName());
                        claims.put(TokenProvider.CLAIM_USER_ID, principal.getUserId().toString());
                        claims.put(TokenProvider.CLAIM_ROLES, principal.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList());

                        token = tokenProvider.createAccessToken(principal.getTokenId().toString(), claims);
                    } else {
                        Map<String, Object> claims = new HashMap<>();
                        claims.put(TokenProvider.CLAIM_ROLES, List.of(CoreMsRoles.SYSTEM));

                        token = tokenProvider.createAccessToken(null, claims);
                    }

                    request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    return execution.execute(request, body);
                });
    }
}
