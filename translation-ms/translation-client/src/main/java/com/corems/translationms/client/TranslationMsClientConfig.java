package com.corems.translationms.client;

import com.corems.translationms.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class TranslationMsClientConfig {

    @Value("${translation.ms.base-url:http://localhost:3003}")
    private String translationMsBaseUrl;

    @Bean(name = "translationRestClient")
    @ConditionalOnMissingBean(name = "translationRestClient")
    public RestClient translationRestClient(RestClient.Builder inboundRestClientBuilder) {
        return inboundRestClientBuilder
                .baseUrl(translationMsBaseUrl)
                .build();
    }

    @Bean(name = "translationMsApiClient")
    @ConditionalOnMissingBean(name = "translationMsApiClient")
    public ApiClient translationMsApiClient(RestClient translationRestClient) {
        ApiClient apiClient = new ApiClient(translationRestClient);
        apiClient.setBasePath(translationMsBaseUrl);
        return apiClient;
    }

    @Bean
    @ConditionalOnMissingBean(name = "translationApi")
    public TranslationApi translationApi(ApiClient translationMsApiClient) throws Exception {
        return new TranslationApi(translationMsApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "translationAdminApi")
    public TranslationAdminApi translationAdminApi(ApiClient translationMsApiClient) throws Exception {
        return new TranslationAdminApi(translationMsApiClient);
    }
}
