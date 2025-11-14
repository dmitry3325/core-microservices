package com.corems.communicationms.client;

import com.corems.communicationms.ApiClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommunicationMsClientConfig {

    @Bean
    @ConditionalOnMissingBean(ApiClient.class)
    public ApiClient userApiClient(@Value("${client.communicationms.base-url:http://localhost:3000}") String baseUrl) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        return apiClient;
    }


}


