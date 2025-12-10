package com.corems.userms.client;

import com.corems.userms.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class UserMsClientConfig {

    @Value("${userms.base-url:http://localhost:3000}")
    private String userMsBaseUrl;

    @Bean(name = "userRestClient")
    @ConditionalOnMissingBean(name = "userRestClient")
    public RestClient userRestClient(RestClient.Builder inboundRestClientBuilder) {
        return inboundRestClientBuilder
                .baseUrl(userMsBaseUrl)
                .build();
    }

    @Bean(name = "userApiClient")
    @ConditionalOnMissingBean(name = "userApiClient")
    public ApiClient userApiClient(RestClient userRestClient) {
        ApiClient apiClient = new ApiClient(userRestClient);
        return apiClient;
    }

    @Bean
    @ConditionalOnMissingBean(name = "userApi")
    public UserApi userApi(ApiClient userApiClient) throws Exception {
       return new UserApi(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "profileApi")
    public ProfileApi profileApi(ApiClient userApiClient) throws Exception {
        return new ProfileApi(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "authenticationApi")
    public AuthenticationApi authenticationApi(ApiClient userApiClient) throws Exception {
        return new AuthenticationApi(userApiClient);
    }

}
