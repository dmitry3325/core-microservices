package com.corems.userms.client;

import com.corems.userms.ApiClient;
import com.corems.userms.client.AuthenticationApi;
import com.corems.userms.client.ProfileApi;
import com.corems.userms.client.UserApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class UserMsClientConfig {

    @Value("${userms.base-url:http://localhost:3000}")
    private String userMsBaseUrl;


    @Bean(name = "userWebClient")
    @ConditionalOnMissingBean(name = "userWebClient")
    public WebClient userWebClient(WebClient.Builder inboundWebClientBuilder) {
        return inboundWebClientBuilder
                .baseUrl(userMsBaseUrl)
                .build();
    }

    @Bean(name = "userApiClient")
    @ConditionalOnMissingBean(name = "userApiClient")
    public ApiClient userApiClient(@Qualifier("userWebClient") WebClient userWebClient) {
        ApiClient apiClient = new ApiClient(userWebClient);
        apiClient.setBasePath(userMsBaseUrl);
        return apiClient;
    }

    // Beans for generated API interfaces
    @Bean
    @ConditionalOnMissingBean(UserApi.class)
    public UserApi userApi(ApiClient userApiClient) {
        return new UserApi(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(ProfileApi.class)
    public ProfileApi profileApi(ApiClient userApiClient) {
        return new ProfileApi(userApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationApi.class)
    public AuthenticationApi authenticationApi(ApiClient userApiClient) {
        return new AuthenticationApi(userApiClient);
    }

}
