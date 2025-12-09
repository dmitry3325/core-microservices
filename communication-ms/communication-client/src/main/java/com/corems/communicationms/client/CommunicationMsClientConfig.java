package com.corems.communicationms.client;

import com.corems.communicationms.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class CommunicationMsClientConfig {

    @Value("${communicationms.base-url:http://localhost:3001}")
    private String communicationBaseUrl;

    @Bean(name = "communicationRestClient")
    @ConditionalOnMissingBean(name = "communicationRestClient")
    public RestClient communicationRestClient(RestClient.Builder inboundRestClientBuilder) {
        return inboundRestClientBuilder
                .baseUrl(communicationBaseUrl)
                .build();
    }

    @Bean(name = "communicationApiClient")
    @ConditionalOnMissingBean(name = "communicationApiClient")
    public ApiClient communicationApiClient(RestClient communicationRestClient) {
        ApiClient apiClient = new ApiClient(communicationRestClient);
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
