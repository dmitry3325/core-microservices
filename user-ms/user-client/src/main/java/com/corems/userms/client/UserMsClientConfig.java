package com.corems.userms.client;

import com.corems.userms.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
        apiClient.setBasePath(userMsBaseUrl);
        return apiClient;
    }

    @Bean
    @ConditionalOnClass(name = "com.corems.userms.client.UserApi")
    @ConditionalOnMissingBean(name = "userApi")
    public Object userApi(ApiClient userApiClient) throws Exception {
        Class<?> apiClass = Class.forName("com.corems.userms.client.UserApi");
        return apiClass.getConstructor(ApiClient.class).newInstance(userApiClient);
    }

    @Bean
    @ConditionalOnClass(name = "com.corems.userms.client.ProfileApi")
    @ConditionalOnMissingBean(name = "profileApi")
    public Object profileApi(ApiClient userApiClient) throws Exception {
        Class<?> apiClass = Class.forName("com.corems.userms.client.ProfileApi");
        return apiClass.getConstructor(ApiClient.class).newInstance(userApiClient);
    }

    @Bean
    @ConditionalOnClass(name = "com.corems.userms.client.AuthenticationApi")
    @ConditionalOnMissingBean(name = "authenticationApi")
    public Object authenticationApi(ApiClient userApiClient) throws Exception {
        Class<?> apiClass = Class.forName("com.corems.userms.client.AuthenticationApi");
        return apiClass.getConstructor(ApiClient.class).newInstance(userApiClient);
    }

}
