package com.corems.translationms.client;

import com.corems.translationms.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
    @ConditionalOnClass(name = "com.corems.translationms.client.TranslationApi")
    @ConditionalOnMissingBean(name = "translationApi")
    public Object translationApi(ApiClient translationMsApiClient) throws Exception {
        Class<?> apiClass = Class.forName("com.corems.translationms.client.TranslationApi");
        return apiClass.getConstructor(ApiClient.class).newInstance(translationMsApiClient);
    }

    @Bean
    @ConditionalOnClass(name = "com.corems.translationms.client.TranslationAdminApi")
    @ConditionalOnMissingBean(name = "translationAdminApi")
    public Object translationAdminApi(ApiClient translationMsApiClient) throws Exception {
        Class<?> apiClass = Class.forName("com.corems.translationms.client.TranslationAdminApi");
        return apiClass.getConstructor(ApiClient.class).newInstance(translationMsApiClient);
    }
}
