package com.corems.userms.client.config;

import com.corems.userms.ApiClient;

import com.corems.userms.client.UserApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class UserMsClientConfig {

    @Bean
    @ConditionalOnMissingBean(ApiClient.class)
    public ApiClient userApiClient(@Value("${client.userms.base-url:http://localhost:3000}") String baseUrl) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        return apiClient;
    }

    @Bean
    @ConditionalOnMissingBean(UserApi.class)
    public UserApi adminUserApi(ApiClient userApiClient) {
        return new UserApi(userApiClient);
    }


}

