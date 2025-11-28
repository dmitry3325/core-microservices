package com.corems.communicationms.client;

import com.corems.communicationms.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class CommunicationMsClientConfig {

    @Value("${communicationms.base-url:http://localhost:3001}")
    private String communicationBaseUrl;

    @Bean(name = "communicationWebClient")
    @ConditionalOnMissingBean(name = "communicationWebClient")
    public WebClient communicationWebClient(WebClient.Builder inboundWebClientBuilder) {
        return inboundWebClientBuilder
                .baseUrl(communicationBaseUrl)
                .build();
    }

    @Bean(name = "communicationApiClient")
    @ConditionalOnMissingBean(name = "communicationApiClient")
    public ApiClient communicationApiClient(@Qualifier("communicationWebClient") WebClient communicationWebClient) {
        ApiClient apiClient = new ApiClient(communicationWebClient);
        apiClient.setBasePath(communicationBaseUrl);
        return apiClient;
    }

    @Bean
    @ConditionalOnMissingBean(MessagesApi.class)
    public MessagesApi messagesApi(ApiClient communicationApiClient) {
        return new MessagesApi(communicationApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationsApi.class)
    public NotificationsApi notificationsApi(ApiClient communicationApiClient) {
        return new NotificationsApi(communicationApiClient);
    }
}
